/*
 * Copyright (c) 2019-2022 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.micro

import cats.Id
import cats.implicits._
import com.snowplowanalytics.iglu.client.IgluCirceClient
import com.snowplowanalytics.iglu.client.resolver.Resolver
import com.snowplowanalytics.iglu.client.resolver.registries.Registry
import com.snowplowanalytics.iglu.core.circe.CirceIgluCodecs._
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaVer, SelfDescribingData}
import com.snowplowanalytics.snowplow.collectors.scalastream.model.{CollectorConfig, SinkConfig}
import com.snowplowanalytics.snowplow.enrich.common.enrichments.EnrichmentRegistry
import com.snowplowanalytics.snowplow.enrich.common.enrichments.registry.EnrichmentConf
import com.snowplowanalytics.snowplow.enrich.common.utils.JsonUtils
import com.snowplowanalytics.snowplow.micro.IdImplicits._
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._
import pureconfig.generic.auto._
import pureconfig.generic.{FieldCoproductHint, ProductHint}
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}

import java.io.File
import java.net.URI
import java.nio.file.{Path, Paths}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import scala.io.Source

/** Contain functions to parse the command line arguments,
  * to parse the configuration for the collector, Akka HTTP and Iglu
  * and to instantiate Iglu client.
  */
private[micro] object ConfigHelper {
  object EnvironmentVariables {
    val igluRegistryUrl = "MICRO_IGLU_REGISTRY_URL"
    val igluApiKey = "MICRO_IGLU_API_KEY"
    val sslCertificatePassword = "MICRO_SSL_CERT_PASSWORD"
  }

  implicit def hint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  // Copied from Enrich - necessary for parsing enrichment configs
  implicit val sinkConfigHint = new FieldCoproductHint[SinkConfig]("enabled")
  type EitherS[A] = Either[String, A]

  case class MicroConfig(
    collectorConfig: CollectorConfig,
    igluResolver: Resolver[Id],
    igluClient: IgluCirceClient[Id],
    enrichmentConfigs: List[EnrichmentConf],
    akkaConfig: Config,
    sslContext: Option[SSLContext],
    outputEnrichedTsv: Boolean
  )

  /** Parse the command line arguments and the configuration files. */
  def parseConfig(args: Array[String]): MicroConfig = {
    case class CommandLineOptions(
      collectorConfigFile: Option[File] = None,
      igluConfigFile: Option[File] = None,
      outputEnrichedTsv: Boolean = false
    )

    def formatEnvironmentVariables(descriptions: (String, String)*): String = {
      val longest = descriptions.map(_._1.length).max
      descriptions.map {
        case (envVar, desc) => s"  $envVar${" " * (longest - envVar.length)}  $desc"
      }.mkString("\n")
    }

    val parser = new scopt.OptionParser[CommandLineOptions](buildinfo.BuildInfo.name) {
      head(buildinfo.BuildInfo.name, buildinfo.BuildInfo.version)
      help("help")
      version("version")
      opt[Option[File]]("collector-config")
        .optional()
        .valueName("<filename>")
        .text("Configuration file for collector")
        .action((f: Option[File], c: CommandLineOptions) => c.copy(collectorConfigFile = f))
        .validate(f =>
          f match {
            case Some(file) =>
              if (file.exists) success
              else failure(s"Configuration file $f does not exist")
            case None => success
          }
        )
      opt[Option[File]]("iglu")
        .optional()
        .valueName("<filename>")
        .text("Configuration file for Iglu Client")
        .action((f: Option[File], c: CommandLineOptions) => c.copy(igluConfigFile = f))
        .validate(f =>
          f match {
            case Some(file) =>
              if (file.exists) success
              else failure(s"Configuration file $f does not exist")
            case None => success
          }
        )
      opt[Unit]('t', "output-tsv")
        .optional()
        .text("Print events in TSV format to standard output")
        .action((_, c: CommandLineOptions) => c.copy(outputEnrichedTsv = true))
      note(
        "\nSupported environment variables:\n\n" + formatEnvironmentVariables(
          EnvironmentVariables.igluRegistryUrl ->
            "The URL for an additional custom Iglu registry",
          EnvironmentVariables.igluApiKey ->
            s"An optional API key for an Iglu registry defined with ${EnvironmentVariables.igluRegistryUrl}",
          EnvironmentVariables.sslCertificatePassword ->
            "The password for the optional SSL/TLS certificate in /config/ssl-certificate.p12. Enables HTTPS"
        )
      )
    }

    val config = parser.parse(args, CommandLineOptions()) getOrElse {
      throw new RuntimeException("Problem while parsing arguments") // should never be called
    }

    val resolved = config.collectorConfigFile match {
      case Some(f) => ConfigFactory.parseFile(f).resolve()
      case None    => ConfigFactory.empty()
    }

    val collectorConfig = ConfigFactory.load(resolved.withFallback(ConfigFactory.load()))

    val resolverSource = config.igluConfigFile match {
      case Some(f) => Source.fromFile(f)
      case None => Source.fromResource("default-iglu-resolver.json")
    }

    val extraRegistry = sys.env.get(EnvironmentVariables.igluRegistryUrl).map { registry =>
      val uri = URI.create(registry)
      Registry.Http(
        Registry.Config(s"Custom ($registry)", 0, List.empty),
        Registry.HttpConnection(uri, sys.env.get(EnvironmentVariables.igluApiKey))
      )
    }

    val (resolver, igluClient) = getIgluClientFromSource(resolverSource, extraRegistry) match {
      case Right(ok) => ok
      case Left(e) =>
        throw new IllegalArgumentException(s"Error while reading Iglu config file: $e.")
    }

    val enrichmentConfigs = Option(getClass.getResource("/enrichments")).map { dir =>
      getEnrichmentRegistryFromPath(Paths.get(dir.toURI), igluClient) match {
        case Right(ok) => ok
        case Left(e) =>
          throw new IllegalArgumentException(s"Error while reading enrichment config file(s): $e.")
      }
    }.getOrElse(List.empty)

    val sslContext = sys.env.get(EnvironmentVariables.sslCertificatePassword).map { password =>
      // Adapted from https://doc.akka.io/docs/akka-http/current/server-side/server-https-support.html.
      // We could use SSLContext.getDefault instead of all of this, but then we would need to
      // force the user to add arcane -D flags when running Micro, which is not the best experience.
      val keystore = KeyStore.getInstance("PKCS12")
      val certificateFile = getClass.getClassLoader.getResourceAsStream("ssl-certificate.p12")
      keystore.load(certificateFile, password.toCharArray)

      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(keystore, password.toCharArray)

      val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
      trustManagerFactory.init(keystore)

      val context = SSLContext.getInstance("TLS")
      context.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom)

      context
    }

    MicroConfig(
      ConfigSource.fromConfig(collectorConfig.getConfig("collector")).loadOrThrow[CollectorConfig],
      resolver,
      igluClient,
      enrichmentConfigs,
      collectorConfig,
      sslContext,
      config.outputEnrichedTsv
    )
  }

  /** Instantiate an Iglu client from its configuration file. */
  def getIgluClientFromSource(igluConfigSource: Source, extraRegistry: Option[Registry]): Either[String, (Resolver[Id], IgluCirceClient[Id])] =
    for {
      text <- Either.catchNonFatal(igluConfigSource.mkString).leftMap(_.getMessage)
      json <- parse(text).leftMap(_.show)
      config <- Resolver.parseConfig(json).leftMap(_.show)
      resolver <- Resolver.fromConfig[Id](config).leftMap(_.show).value
      completeResolver = resolver.copy(repos = resolver.repos ++ extraRegistry)
    } yield (completeResolver, IgluCirceClient.fromResolver[Id](completeResolver, config.cacheSize))

  def getEnrichmentRegistryFromPath(path: Path, igluClient: IgluCirceClient[Id]) = {
    val schemaKey = SchemaKey(
      "com.snowplowanalytics.snowplow",
      "enrichments",
      "jsonschema",
      SchemaVer.Full(1, 0, 0)
    )
    // Loosely adapted from Enrich#localEnrichmentConfigsExtractor
    val directory = Option(path.toFile.listFiles).fold(List.empty[File])(_.toList)
    val configs = directory
      .filter(_.getName.endsWith(".json"))
      .map(scala.io.Source.fromFile(_).mkString)
      .map(JsonUtils.extractJson).sequence[EitherS, Json]
      .map(jsonConfigs => SelfDescribingData[Json](schemaKey, Json.fromValues(jsonConfigs)).asJson)
      .flatMap { jsonConfig =>
        EnrichmentRegistry.parse(jsonConfig, igluClient, localMode = false)
          .leftMap(_.toList.mkString("; ")).toEither
      }
    val scripts = directory
      .filter(_.getName.endsWith(".js"))
      .map(scala.io.Source.fromFile(_).mkString)
      .map(EnrichmentConf.JavascriptScriptConf(schemaKey, _))
    configs.map(scripts ::: _)
  }
}
