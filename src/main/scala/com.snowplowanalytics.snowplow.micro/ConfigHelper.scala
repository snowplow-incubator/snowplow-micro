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

import com.typesafe.config.{Config, ConfigFactory}

import pureconfig.{ConfigFieldMapping, CamelCase, ConfigSource}
import pureconfig.generic.{ProductHint, FieldCoproductHint}
import pureconfig.generic.auto._

import cats.Id
import cats.implicits._

import io.circe.parser.parse

import scala.io.Source

import java.io.File

import com.snowplowanalytics.iglu.client.IgluCirceClient
import com.snowplowanalytics.iglu.client.resolver.Resolver
import com.snowplowanalytics.iglu.client.resolver.registries.Registry
import com.snowplowanalytics.snowplow.collectors.scalastream.model.{CollectorConfig, SinkConfig}

import java.net.URI

/** Contain functions to parse the command line arguments,
  * to parse the configuration for the collector, Akka HTTP and Iglu
  * and to instantiate Iglu client.
  */
private[micro] object ConfigHelper {
  object EnvironmentVariables {
    val igluRegistryUrl = "MICRO_IGLU_REGISTRY_URL"
    val igluApiKey = "MICRO_IGLU_API_KEY"
  }

  implicit def hint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  implicit val sinkConfigHint = new FieldCoproductHint[SinkConfig]("enabled")

  /** Parse the command line arguments and the configuration files. */
  def parseConfig(args: Array[String]): (CollectorConfig, Resolver[Id], IgluCirceClient[Id], Config, Boolean) = {
    case class MicroConfig(
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

    val parser = new scopt.OptionParser[MicroConfig](buildinfo.BuildInfo.name) {
      head(buildinfo.BuildInfo.name, buildinfo.BuildInfo.version)
      help("help")
      version("version")
      opt[Option[File]]("collector-config")
        .optional()
        .valueName("<filename>")
        .text("Configuration file for collector")
        .action((f: Option[File], c: MicroConfig) => c.copy(collectorConfigFile = f))
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
        .action((f: Option[File], c: MicroConfig) => c.copy(igluConfigFile = f))
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
        .action((_, c: MicroConfig) => c.copy(outputEnrichedTsv = true))
      note(
        "\nSupported environment variables:\n\n" + formatEnvironmentVariables(
          EnvironmentVariables.igluRegistryUrl ->
            "The URL for an additional custom Iglu registry",
          EnvironmentVariables.igluApiKey ->
            s"An optional API key for an Iglu registry defined with ${EnvironmentVariables.igluRegistryUrl}"
        )
      )
    }

    val config = parser.parse(args, MicroConfig()) getOrElse {
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

    (
      ConfigSource.fromConfig(collectorConfig.getConfig("collector")).loadOrThrow[CollectorConfig],
      resolver,
      igluClient,
      collectorConfig,
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
}
