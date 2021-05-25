/*
 * Copyright (c) 2019-2020 Snowplow Analytics Ltd. All rights reserved.
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

import io.circe.Json
import io.circe.parser.parse

import scala.io.Source

import java.io.File

import com.snowplowanalytics.iglu.client.Client
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
  def parseConfig(args: Array[String]): (CollectorConfig, Client[Id, Json], Config) = {
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
        .text("Configuration file for Iglu igluClient")
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

    val igluClient = getIgluClientFromFile(igluFile) match {
      case Right(igluClient) => igluClient
      case Left(e) =>
        throw new IllegalArgumentException(s"Error while reading Iglu config file: $e.")
    }

    (
      ConfigSource.fromConfig(collectorConfig.getConfig("collector")).loadOrThrow[CollectorConfig],
      igluClient,
      collectorConfig
    )
  }

  /** Instantiate an Iglu client from its configuration file. */
  def getIgluClientFromFile(igluConfigFile: File): Either[String, Client[Id, Json]] =
    parse(Source.fromFile(igluConfigFile).mkString)
      .leftMap(_.show)
      .flatMap(json =>
        Client.parseDefault[Id](json).value
          .leftMap(_.getMessage())
      )
}
