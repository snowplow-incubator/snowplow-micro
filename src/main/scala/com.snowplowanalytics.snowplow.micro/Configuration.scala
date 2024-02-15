/**
 * Copyright (c) 2013-present Snowplow Analytics Ltd.
 * All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.0
 * located at https://docs.snowplow.io/limited-use-license-1.0
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
 * OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
 */
package com.snowplowanalytics.snowplow.micro

import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import com.monovore.decline.Opts
import com.snowplowanalytics.iglu.client.IgluCirceClient
import com.snowplowanalytics.iglu.client.resolver.Resolver
import com.snowplowanalytics.iglu.client.resolver.Resolver.ResolverConfig
import com.snowplowanalytics.iglu.client.resolver.registries.{JavaNetRegistryLookup, Registry}
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.snowplow.collector.core.{Config => CollectorConfig}
import com.snowplowanalytics.snowplow.enrich.common.enrichments.EnrichmentRegistry
import com.snowplowanalytics.snowplow.enrich.common.enrichments.registry.EnrichmentConf
import com.typesafe.config.{ConfigFactory, ConfigParseOptions, Config => TypesafeConfig}
import fs2.io.file.{Files, Path => FS2Path}
import io.circe.config.syntax.CirceConfigOps
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Json}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.URI
import java.nio.file.{Path, Paths}

object Configuration {

  object Cli {
    final case class Config(collector: Option[Path], iglu: Option[Path], outputEnrichedTsv: Boolean)
    
    private val collector = Opts.option[Path]("collector-config", "Path to HOCON configuration (optional)", "c", "config.hocon").orNone
    private val iglu = Opts.option[Path]("iglu", "Configuration file for Iglu Client", "i", "iglu.json").orNone
    private val outputEnrichedTsv = Opts.flag("output-tsv", "Print events in TSV format to standard output", "t").orFalse
    
    val config: Opts[Config] = (collector, iglu, outputEnrichedTsv).mapN(Config.apply) 
  }
  

  object EnvironmentVariables {
    val igluRegistryUrl = "MICRO_IGLU_REGISTRY_URL"
    val igluApiKey = "MICRO_IGLU_API_KEY"
    val sslCertificatePassword = "MICRO_SSL_CERT_PASSWORD"
  }

  final case class DummySinkConfig()
  type SinkConfig = DummySinkConfig 
  implicit val dec: Decoder[DummySinkConfig] = Decoder.instance(_ => Right(DummySinkConfig()))

  final case class MicroConfig(collector: CollectorConfig[SinkConfig],
                               iglu: IgluResources,
                               enrichmentsConfig: List[EnrichmentConf],
                               outputEnrichedTsv: Boolean)

  final case class IgluResources(resolver: Resolver[IO], client: IgluCirceClient[IO])

  implicit private def logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def load(): Opts[EitherT[IO, String, MicroConfig]] = {
    Cli.config.map { cliConfig =>
      for {
        collectorConfig <- loadCollectorConfig(cliConfig.collector)
        igluResources <- loadIgluResources(cliConfig.iglu)
        enrichmentsConfig <- loadEnrichmentConfig(igluResources.client)
      } yield MicroConfig(collectorConfig, igluResources, enrichmentsConfig, cliConfig.outputEnrichedTsv)
    }
  }

  def loadCollectorConfig(path: Option[Path]): EitherT[IO, String, CollectorConfig[SinkConfig]] = {
    val resolveOrder = (config: TypesafeConfig) =>
      namespaced(ConfigFactory.load(namespaced(config.withFallback(namespaced(ConfigFactory.parseResources("collector-micro.conf"))))))

    loadConfig[CollectorConfig[SinkConfig]](path, resolveOrder)
  }

  def loadIgluResources(path: Option[Path]): EitherT[IO, String, IgluResources] = {
    val resolveOrder = (config: TypesafeConfig) =>
      config.withFallback(ConfigFactory.parseResources("default-iglu-resolver.conf"))

    loadConfig[ResolverConfig](path, resolveOrder)
      .flatMap(buildIgluResources)
  }

  def loadEnrichmentConfig(igluClient: IgluCirceClient[IO]): EitherT[IO, String, List[EnrichmentConf]] = {
    Option(getClass.getResource("/enrichments")) match {
      case Some(definedEnrichments) =>
        val path = Paths.get(definedEnrichments.toURI)
        for {
          asJson <- loadEnrichmentsAsSDD(path, igluClient, fileType = ".json")
          asHocon <- loadEnrichmentsAsSDD(path, igluClient, fileType = ".hocon")
          asJSScripts <- loadJSScripts(path)
        } yield asJson ::: asHocon ::: asJSScripts 
      case None =>
        EitherT.rightT[IO, String](List.empty)
    }
  }

  private def buildIgluResources(resolverConfig: ResolverConfig): EitherT[IO, String, IgluResources] =
    for {
      resolver <- Resolver.fromConfig[IO](resolverConfig).leftMap(_.show)
      completeResolver = resolver.copy(repos = resolver.repos ++ readIgluExtraRegistry())
      client <- EitherT.liftF(IgluCirceClient.fromResolver[IO](completeResolver, resolverConfig.cacheSize))
    } yield IgluResources(resolver, client)


  private def loadEnrichmentsAsSDD(enrichmentsDirectory: Path, 
                                   igluClient: IgluCirceClient[IO],
                                   fileType: String): EitherT[IO, String, List[EnrichmentConf]] = {
    listAvailableEnrichments(enrichmentsDirectory, fileType)
      .flatMap(loadEnrichmentsAsJsons)
      .map(asSDD)
      .flatMap(parseEnrichments(igluClient))
  }

  private def loadJSScripts(enrichmentsDirectory: Path): EitherT[IO, String, List[EnrichmentConf]] = EitherT.right {
    listFiles(enrichmentsDirectory, fileType = ".js")
      .flatMap { scripts =>
        scripts.traverse(buildJSConfig)
      }
  }

  private def buildJSConfig(script: FS2Path): IO[EnrichmentConf.JavascriptScriptConf] = {
    Files[IO]
      .readUtf8Lines(script)
      .compile
      .toList
      .map(lines => EnrichmentConf.JavascriptScriptConf(null, lines.mkString("\n")))
  }

  private def listAvailableEnrichments(enrichmentsDirectory: Path, fileType: String) = {
    listFiles(enrichmentsDirectory, fileType)
      .flatTap(files => logger.info(s"Files found in $enrichmentsDirectory: ${files.mkString(", ")}"))
      .attemptT
      .leftMap(e => show"Cannot list ${enrichmentsDirectory.toAbsolutePath.toString} directory with JSON: ${e.getMessage}")
  }

  private def listFiles(path: Path, fileType: String): IO[List[FS2Path]] = {
    Files[IO].list(fs2.io.file.Path.fromNioPath(path))
      .filter(path => path.toString.endsWith(fileType))
      .compile
      .toList
  }

  private def loadEnrichmentsAsJsons(enrichments: List[FS2Path]): EitherT[IO, String, List[Json]] = {
    enrichments.traverse { enrichmentPath =>
      loadConfig[Json](Some(enrichmentPath.toNioPath), identity)
    }
  }

  private def asSDD(jsons: List[Json]): SelfDescribingData[Json] = {
    val schema = SchemaKey("com.snowplowanalytics.snowplow", "enrichments", "jsonschema", SchemaVer.Full(1, 0, 0))
    SelfDescribingData(schema, Json.arr(jsons: _*))
  }

  private def parseEnrichments(igluClient: IgluCirceClient[IO])(sdd: SelfDescribingData[Json]): EitherT[IO, String, List[EnrichmentConf]] =
    EitherT {
      EnrichmentRegistry
        .parse[IO](sdd.asJson, igluClient, localMode = false, registryLookup = JavaNetRegistryLookup.ioLookupInstance[IO])
        .map(_.toEither)
    }.leftMap { x =>
      show"Cannot decode enrichments - ${x.mkString_(", ")}"
    }

  private def readIgluExtraRegistry(): Option[Registry.Http] = {
    sys.env.get(EnvironmentVariables.igluRegistryUrl).map { registry =>
      val uri = URI.create(registry)
      Registry.Http(
        Registry.Config(s"Custom ($registry)", 0, List.empty),
        Registry.HttpConnection(uri, sys.env.get(EnvironmentVariables.igluApiKey))
      )
    }
  }

  private def loadConfig[A: Decoder](path: Option[Path],
                                     load: TypesafeConfig => TypesafeConfig): EitherT[IO, String, A] = EitherT {
    IO {
      for {
        config <- Either.catchNonFatal(handleInputPath(path)).leftMap(_.getMessage)
        config <- Either.catchNonFatal(config.resolve()).leftMap(_.getMessage)
        config <- Either.catchNonFatal(load(config)).leftMap(_.getMessage)
        parsed <- config.as[A].leftMap(_.show)
      } yield parsed
    }
  }

  private def handleInputPath(path: Option[Path]): TypesafeConfig = {
    path match {
      case Some(definedPath) => 
        //Fail when provided file doesn't exist
        ConfigFactory.parseFile(definedPath.toFile, ConfigParseOptions.defaults().setAllowMissing(false))
      case None => ConfigFactory.empty()
    }
  }

  private def namespaced(config: TypesafeConfig): TypesafeConfig = {
    val namespace = "collector"
    if (config.hasPath(namespace))
      config.getConfig(namespace).withFallback(config.withoutPath(namespace))
    else
      config
  }

  implicit val resolverDecoder: Decoder[ResolverConfig] = Decoder.decodeJson.emap(json => Resolver.parseConfig(json).leftMap(_.show))

}
