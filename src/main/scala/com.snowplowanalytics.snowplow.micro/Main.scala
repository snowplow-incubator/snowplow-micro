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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import cats.Id

import io.circe.Json

import org.slf4j.LoggerFactory

import com.typesafe.config.Config

import com.snowplowanalytics.iglu.client.Client

import com.snowplowanalytics.snowplow.collectors.scalastream.model.{CollectorConfig, CollectorSinks}

/** Read the configuration and instantiate Snowplow Micro,
  * which acts as a `Collector` and has an in-memory sink
  * holding the valid and invalid events.
  * It offers an HTTP endpoint to query this sink.
  */ 
object Main {
  lazy val logger = LoggerFactory.getLogger(getClass())

  def main(args: Array[String]): Unit = {
    val (collectorConf, igluClient, akkaConf) = ConfigHelper.parseConfig(args)
    run(collectorConf, igluClient, akkaConf)
  }

  /** Create the in-memory sink,
    * get the endpoints for both the collector and to query Snowplow Micro,
    * and start the HTTP server.
    */
  def run(
    collectorConf: CollectorConfig,
    igluClient: Client[Id, Json],
    akkaConf: Config
  ): Unit = {
    implicit val system = ActorSystem.create("snowplow-micro", akkaConf)
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val sinks = CollectorSinks(MemorySink(igluClient), MemorySink(igluClient))
    val igluService = new IgluService(igluClient)

    val routes = Routing.getMicroRoutes(collectorConf, sinks, igluService)

    Http()
      .bindAndHandle(routes, collectorConf.interface, collectorConf.port)
      .foreach { binding =>
        logger.info(s"REST interface bound to ${binding.localAddress}")
      }
  }
}
