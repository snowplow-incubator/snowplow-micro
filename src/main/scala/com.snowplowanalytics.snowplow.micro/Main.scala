/*
 * Copyright (c) 2019-2023 Snowplow Analytics Ltd. All rights reserved.
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

import java.io.File

import org.slf4j.LoggerFactory

import scala.sys.process._

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}

import cats.Id

import com.snowplowanalytics.snowplow.collectors.scalastream.model.CollectorSinks

import com.snowplowanalytics.snowplow.enrich.common.enrichments.EnrichmentRegistry
import com.snowplowanalytics.snowplow.enrich.common.enrichments.registry.{Enrichment, EnrichmentConf}
import com.snowplowanalytics.snowplow.enrich.common.utils.{BlockerF, ShiftExecution}

import com.snowplowanalytics.snowplow.micro.ConfigHelper.MicroConfig
import com.snowplowanalytics.snowplow.micro.utils._

/** Read the configuration and instantiate Snowplow Micro,
  * which acts as a `Collector` and has an in-memory sink
  * holding the valid and invalid events.
  * It offers an HTTP endpoint to query this sink.
  */
object Main {
  lazy val logger = LoggerFactory.getLogger(getClass())

  def main(args: Array[String]): Unit = {
    val config = ConfigHelper.parseConfig(args)
    run(config)
  }

  def setupEnrichments(configs: List[EnrichmentConf]): EnrichmentRegistry[Id] = {
    configs.flatMap(_.filesToCache).foreach { case (uri, location) =>
      logger.info(s"Downloading ${uri}...")
      uri.toURL #> new File(location) !!
    }

    val enrichmentRegistry = EnrichmentRegistry.build[Id](configs, BlockerF.noop, ShiftExecution.noop).value match {
      case Right(ok) => ok
      case Left(e) =>
        throw new IllegalArgumentException(s"Error while enabling enrichments: $e.")
    }

    val loadedEnrichments = enrichmentRegistry.productIterator.toList.collect {
      case Some(e: Enrichment) => e.getClass.getSimpleName
    }
    if (loadedEnrichments.nonEmpty) {
      logger.info(s"Enabled enrichments: ${loadedEnrichments.mkString(", ")}")
    } else {
      logger.info(s"No enrichments enabled.")
    }

    enrichmentRegistry
  }

  /** Create the in-memory sink,
    * get the endpoints for both the collector and to query Snowplow Micro,
    * and start the HTTP server.
    */
  def run(config: MicroConfig): Unit = {
    implicit val system = ActorSystem.create("snowplow-micro", config.akkaConfig)
    implicit val executionContext = system.dispatcher

    val enrichmentRegistry = setupEnrichments(config.enrichmentConfigs)
    val sinks = CollectorSinks(
      MemorySink(config.igluClient, enrichmentRegistry, config.outputEnrichedTsv),
      MemorySink(config.igluClient, enrichmentRegistry, config.outputEnrichedTsv)
    )
    val igluService = new IgluService(config.igluResolver)

    val routes = Routing.getMicroRoutes(config.collectorConfig, sinks, igluService)

    Http()
      .newServerAt(config.collectorConfig.interface, config.collectorConfig.port)
      .bind(routes)
      .foreach { binding =>
        logger.info(s"REST interface bound to ${binding.localAddress}")
      }

    config.sslContext.foreach { sslContext =>
      Http()
        .newServerAt(config.collectorConfig.interface, config.collectorConfig.ssl.port)
        .enableHttps(ConnectionContext.httpsServer(sslContext))
        .bind(routes)
        .foreach { binding =>
          logger.info(s"HTTPS REST interface bound to ${binding.localAddress}")
        }
    }
  }
}
