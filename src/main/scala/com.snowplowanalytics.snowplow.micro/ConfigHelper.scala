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

import com.snowplowanalytics.snowplow.collectors.scalastream.model.{CollectorConfig, SinkConfig}

/** Contain functions to parse the command line arguments,
  * to parse the configuration for the collector, Akka HTTP and Iglu
  * and to instantiate Iglu client.
  */
private[micro] object ConfigHelper {

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
        .text("Configuration file for Iglu igluClient")
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
    }
    
    val (collectorFile, igluFile, outputEnrichedTsvEnabled) = parser.parse(args, MicroConfig()) match {
      case Some(microConfig: MicroConfig) =>
        (microConfig.collectorConfigFile, microConfig.igluConfigFile, microConfig.outputEnrichedTsv)
      case None =>
        throw new RuntimeException("Problem while parsing arguments") // should never be called
    }

    val resolved = collectorFile match {
      case Some(f) => ConfigFactory.parseFile(f).resolve()
      case None    => ConfigFactory.empty()
    }

    val collectorConfig = ConfigFactory.load(resolved.withFallback(ConfigFactory.load()))

    val resolverSource = igluFile match {
      case Some(f) => Source.fromFile(f)
      case None => Source.fromResource("default-iglu-resolver.json")
    }

    val (resolver, igluClient) = getIgluClientFromSource(resolverSource) match {
      case Right(ok) => ok
      case Left(e) =>
        throw new IllegalArgumentException(s"Error while reading Iglu config file: $e.")
    }

    (
      ConfigSource.fromConfig(collectorConfig.getConfig("collector")).loadOrThrow[CollectorConfig],
      resolver,
      igluClient,
      collectorConfig,
      outputEnrichedTsvEnabled
    )
  }

  /** Instantiate an Iglu client from its configuration file. */
  def getIgluClientFromSource(igluConfigSource: Source): Either[String, (Resolver[Id], IgluCirceClient[Id])] =
    for {
      text <- Either.catchNonFatal(igluConfigSource.mkString).leftMap(_.getMessage)
      json <- parse(text).leftMap(_.show)
      config <- Resolver.parseConfig(json).leftMap(_.show)
      resolver <- Resolver.fromConfig[Id](config).leftMap(_.show).value
    } yield (resolver, IgluCirceClient.fromResolver[Id](resolver, config.cacheSize))
}
