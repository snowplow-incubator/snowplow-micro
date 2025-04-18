/*
 * Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.1
 * located at https://docs.snowplow.io/limited-use-license-1.1
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
import com.snowplowanalytics.snowplow.enrich.common.adapters.{CallrailSchemas, CloudfrontAccessLogSchemas, GoogleAnalyticsSchemas, HubspotSchemas, MailchimpSchemas, MailgunSchemas, MandrillSchemas, MarketoSchemas, OlarkSchemas, PagerdutySchemas, PingdomSchemas, SendgridSchemas, StatusGatorSchemas, UnbounceSchemas, UrbanAirshipSchemas, VeroSchemas, AdaptersSchemas => EnrichAdaptersSchemas}
import com.snowplowanalytics.snowplow.enrich.common.enrichments.{AtomicFields, EnrichmentRegistry}
import com.snowplowanalytics.snowplow.enrich.common.enrichments.registry.EnrichmentConf
import com.typesafe.config.{ConfigFactory, ConfigParseOptions, Config => TypesafeConfig}
import fs2.io.file.{Files, Path => FS2Path}
import io.circe.config.syntax.CirceConfigOps
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Json, JsonObject}
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
                               enrichConfig: EnrichConfig,
                               outputEnrichedTsv: Boolean)

  final case class EnrichValidation(atomicFieldsLimits: AtomicFields)
  final case class EnrichConfig(
    adaptersSchemas: EnrichAdaptersSchemas,
    maxJsonDepth: Int,
    validation: EnrichValidation
  )

  final case class IgluResources(resolver: Resolver[IO], client: IgluCirceClient[IO])

  implicit private def logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def load(): Opts[EitherT[IO, String, MicroConfig]] = {
    Cli.config.map { cliConfig =>
      for {
        collectorConfig <- loadCollectorConfig(cliConfig.collector)
        enrichConfig <- loadEnrichConfig()
        igluResources <- loadIgluResources(cliConfig.iglu, enrichConfig.maxJsonDepth)
        enrichmentsConfig <- loadEnrichmentConfig(igluResources.client)
      } yield MicroConfig(collectorConfig, igluResources, enrichmentsConfig, enrichConfig, cliConfig.outputEnrichedTsv)
    }
  }

  private def loadCollectorConfig(path: Option[Path]): EitherT[IO, String, CollectorConfig[SinkConfig]] = {
    val resolveOrder = (config: TypesafeConfig) =>
      namespaced(ConfigFactory.load(namespaced(config.withFallback(namespaced(ConfigFactory.parseResources("collector-micro.conf"))))))

    loadConfig[CollectorConfig[SinkConfig]](path, resolveOrder)
  }

  private def loadIgluResources(path: Option[Path], maxJsonDepth: Int): EitherT[IO, String, IgluResources] = {
    val resolveOrder = (config: TypesafeConfig) =>
      config.withFallback(ConfigFactory.parseResources("default-iglu-resolver.conf"))

    loadConfig[ResolverConfig](path, resolveOrder)
      .flatMap(resolverConfig => buildIgluResources(resolverConfig, maxJsonDepth))
  }

  private def loadEnrichmentConfig(igluClient: IgluCirceClient[IO]): EitherT[IO, String, List[EnrichmentConf]] = {
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

  def loadEnrichConfig(): EitherT[IO, String, EnrichConfig] = {
    val resolveOrder = (config: TypesafeConfig) => ConfigFactory.load(config)

    //It's not configurable in micro, we load it from reference.conf provided by enrich
    loadConfig[EnrichConfig](path = None, resolveOrder)
  }

  private def buildIgluResources(resolverConfig: ResolverConfig, maxJsonDepth: Int): EitherT[IO, String, IgluResources] =
    for {
      resolver <- Resolver.fromConfig[IO](resolverConfig).leftMap(_.show)
      completeResolver = resolver.copy(repos = resolver.repos ++ readIgluExtraRegistry())
      client <- EitherT.liftF(IgluCirceClient.fromResolver[IO](completeResolver, resolverConfig.cacheSize, maxJsonDepth))
    } yield IgluResources(completeResolver, client)

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
    val schemaKey = SchemaKey("com.snowplowanalytics.snowplow", "javascript_script_config", "jsonschema", SchemaVer.Full(1, 0, 0))
    Files[IO]
      .readUtf8Lines(script)
      .compile
      .toList
      .map(lines => EnrichmentConf.JavascriptScriptConf(schemaKey, lines.mkString("\n"), JsonObject.empty))
  }

  private def listAvailableEnrichments(enrichmentsDirectory: Path, fileType: String) = {
    listFiles(enrichmentsDirectory, fileType)
      .attemptT
      .leftMap(e => show"Cannot list ${enrichmentsDirectory.toAbsolutePath.toString} directory with JSON: ${e.getMessage}")
  }

  private def listFiles(path: Path, fileType: String): IO[List[FS2Path]] = {
    Files[IO].list(fs2.io.file.Path.fromNioPath(path))
      .filter(path => path.toString.endsWith(fileType))
      .compile
      .toList
      .flatTap(files => logger.info(s"Files with extension: '$fileType' found in $path: ${files.mkString("[", ", ", "]")}"))
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

  implicit val enrichConfigDecoder: Decoder[EnrichConfig] =
    deriveDecoder[EnrichConfig]
  implicit val enrichAdaptersSchemasDecoder: Decoder[EnrichAdaptersSchemas] =
    deriveDecoder[EnrichAdaptersSchemas]
  implicit val callrailSchemasDecoder: Decoder[CallrailSchemas] =
    deriveDecoder[CallrailSchemas]
  implicit val cloudfrontAccessLogSchemasDecoder: Decoder[CloudfrontAccessLogSchemas] =
    deriveDecoder[CloudfrontAccessLogSchemas]
  implicit val googleAnalyticsSchemasDecoder: Decoder[GoogleAnalyticsSchemas] =
    deriveDecoder[GoogleAnalyticsSchemas]
  implicit val hubspotSchemasDecoder: Decoder[HubspotSchemas] =
    deriveDecoder[HubspotSchemas]
  implicit val mailchimpSchemasDecoder: Decoder[MailchimpSchemas] =
    deriveDecoder[MailchimpSchemas]
  implicit val mailgunSchemasDecoder: Decoder[MailgunSchemas] =
    deriveDecoder[MailgunSchemas]
  implicit val mandrillSchemasDecoder: Decoder[MandrillSchemas] =
    deriveDecoder[MandrillSchemas]
  implicit val marketoSchemasDecoder: Decoder[MarketoSchemas] =
    deriveDecoder[MarketoSchemas]
  implicit val olarkSchemasDecoder: Decoder[OlarkSchemas] =
    deriveDecoder[OlarkSchemas]
  implicit val pagerdutySchemasDecoder: Decoder[PagerdutySchemas] =
    deriveDecoder[PagerdutySchemas]
  implicit val pingdomSchemasDecoder: Decoder[PingdomSchemas] =
    deriveDecoder[PingdomSchemas]
  implicit val sendgridSchemasDecoder: Decoder[SendgridSchemas] =
    deriveDecoder[SendgridSchemas]
  implicit val statusgatorSchemasDecoder: Decoder[StatusGatorSchemas] =
    deriveDecoder[StatusGatorSchemas]
  implicit val unbounceSchemasDecoder: Decoder[UnbounceSchemas] =
    deriveDecoder[UnbounceSchemas]
  implicit val urbanAirshipSchemasDecoder: Decoder[UrbanAirshipSchemas] =
    deriveDecoder[UrbanAirshipSchemas]
  implicit val veroSchemasDecoder: Decoder[VeroSchemas] =
    deriveDecoder[VeroSchemas]

  implicit val validationDecoder: Decoder[EnrichValidation] =
    deriveDecoder[EnrichValidation]
  implicit val atomicFieldsDecoder: Decoder[AtomicFields] = Decoder[Map[String, Int]].emap { fieldsLimits =>
    val configuredFields = fieldsLimits.keys.toList
    val supportedFields = AtomicFields.supportedFields.map(_.name)
    val unsupportedFields = configuredFields.diff(supportedFields)

    if (unsupportedFields.nonEmpty)
      Left(s"""
        |Configured atomic fields: ${unsupportedFields.mkString("[", ",", "]")} are not supported.
        |Supported fields: ${supportedFields.mkString("[", ",", "]")}""".stripMargin)
    else
      Right(AtomicFields.from(fieldsLimits))
  }
}
