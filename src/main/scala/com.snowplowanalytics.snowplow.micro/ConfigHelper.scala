/*
 * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.iglu.client.Resolver
import com.snowplowanalytics.snowplow.enrich.common.utils.JsonUtils
import com.snowplowanalytics.snowplow.collectors.scalastream.model.{
  CollectorConfig,
  SinkConfig
}
import com.typesafe.config.{Config, ConfigFactory}
import pureconfig.{
  loadConfigOrThrow,
  ProductHint,
  ConfigFieldMapping,
  CamelCase,
  FieldCoproductHint
}
import scalaz.{Validation, Success, Failure}
import scala.io.Source
import java.io.File

/** Contain functions to parse the command line arguments and the configuration for:
  * - the collector;
  * - Akka HTTP;
  * - Iglu resolver.
  */
private[micro] object ConfigHelper {

  /** Parse the command line arguments and the configuration files. */
  def parseConfig(args: Array[String]): (CollectorConfig, Resolver, Config) = {
    case class MicroConfig(
      collectorConfigFile: File = new File("."),
      igluConfigFile: File = new File(".")
    )

    val parser = new scopt.OptionParser[MicroConfig](buildinfo.BuildInfo.name) {
      head(buildinfo.BuildInfo.name, buildinfo.BuildInfo.version)
      help("help")
      version("version")
      opt[File]("collector-config")
        .required()
        .valueName("<filename>")
        .text("Configuration file for collector")
        .action((f: File, c: MicroConfig) => c.copy(collectorConfigFile = f))
        .validate(f =>
          if (f.exists) success
          else failure(s"Configuration file $f does not exist"))
      opt[File]("iglu")
        .required()
        .valueName("<filename>")
        .text("Configuration file for Iglu resolver")
        .action((f: File, c: MicroConfig) => c.copy(igluConfigFile = f))
        .validate(f =>
          if (f.exists) success
          else failure(s"Configuration file $f does not exist"))
    }

    val (collectorFile, igluFile) = parser.parse(args, MicroConfig()) match {
      case Some(microConfig) =>
        (microConfig.collectorConfigFile, microConfig.igluConfigFile)
      case None =>
        throw new RuntimeException("Problem while parsing arguments") // should never be called
    }

    val collectorConfig = ConfigFactory.parseFile(collectorFile).resolve()
    if (!collectorConfig.hasPath("collector"))
      throw new IllegalArgumentException("Config file for collector doesn't contain \"collector\" path")

    val resolver = getResolverFromFile(igluFile) match {
      case Success(resolver) => resolver
      case Failure(e) =>
        throw new IllegalArgumentException(s"Error while reading Iglu config file: $e.")
    }

    implicit def hint[T] =
      ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    implicit val sinkConfigHint = new FieldCoproductHint[SinkConfig]("enabled")
    (
      loadConfigOrThrow[CollectorConfig](collectorConfig.getConfig("collector")),
      resolver,
      collectorConfig
    )
  }

  /** Instantiate an Iglu resolver from its configuration file. */
  def getResolverFromFile(igluConfigFile: File): Validation[String, Resolver] = {
    val fileContent = Source.fromFile(igluConfigFile).mkString
    JsonUtils
      .extractJson("", fileContent)
      .flatMap(json => Resolver.parse(json))
      .leftMap(_.toString)
  }
}
