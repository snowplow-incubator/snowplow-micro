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

import java.io.File

import scala.sys.process._

import org.slf4j.LoggerFactory

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}

import cats.implicits._

import cats.effect.{Blocker, ContextShift, ExitCode, IO, IOApp, Sync}

import com.snowplowanalytics.snowplow.collectors.scalastream.model.CollectorSinks

import com.snowplowanalytics.forex.ZonedClock

import com.snowplowanalytics.snowplow.enrich.common.enrichments.EnrichmentRegistry
import com.snowplowanalytics.snowplow.enrich.common.enrichments.registry.{Enrichment, EnrichmentConf}
import com.snowplowanalytics.snowplow.enrich.common.utils.BlockerF

import com.snowplowanalytics.snowplow.micro.ConfigHelper.MicroConfig

/** Read the configuration and instantiate Snowplow Micro,
  * which acts as a `Collector` and has an in-memory sink
  * holding the valid and invalid events.
  * It offers an HTTP endpoint to query this sink.
  */
object Main extends IOApp {
  lazy val logger = LoggerFactory.getLogger(getClass())

  def run(args: List[String]): IO[ExitCode] = {
    val config = ConfigHelper.parseConfig(args.toArray)
    run(config)
  }

  def setupEnrichments(
    blocker: Blocker,
    configs: List[EnrichmentConf]
  ): IO[EnrichmentRegistry[IO]] = {
    val maybeRegistry = for {
      registry <- EnrichmentRegistry.build[IO](configs, BlockerF.ofBlocker[IO](blocker))
      _ <- configs.flatMap(_.filesToCache).traverse_ { case (uri, location) =>
             IO(logger.info(s"Downloading ${uri}...")) >>
               blocker.blockOn(IO(uri.toURL #> new File(location) !!))
           }
      enabledEnrichments = registry.productIterator.toList.collect {
        case Some(e: Enrichment) => e.getClass.getSimpleName
      }
      ///_ <- log
    } yield registry


    //if (loadedEnrichments.nonEmpty) {
    //  logger.info(s"Enabled enrichments: ${loadedEnrichments.mkString(", ")}")
    //} else {
    //  logger.info(s"No enrichments enabled.")
    //}

    maybeRegistry
  }

  /** Create the in-memory sink,
    * get the endpoints for both the collector and to query Snowplow Micro,
    * and start the HTTP server.
    */
  def run(config: MicroConfig): IO[ExitCode] = Blocker[IO].use { blocker =>
    implicit val system = ActorSystem.create("snowplow-micro", config.akkaConfig)
    implicit val executionContext = system.dispatcher

    val enrichmentRegistry = setupEnrichments[IO](blocker, config.enrichmentConfigs)
    val sinks = CollectorSinks(
      MemorySink(config.igluClient, enrichmentRegistry, config.outputEnrichedTsv),
      MemorySink(config.igluClient, enrichmentRegistry, config.outputEnrichedTsv)
    )
    val igluService = new IgluService(config.igluResolver)

    val routes = Routing.getMicroRoutes(config.collectorConfig, sinks, igluService)

    val http = IO(
      Http()
        .newServerAt(config.collectorConfig.interface, config.collectorConfig.port)
        .bind(routes)
        .foreach { binding =>
          logger.info(s"REST interface bound to ${binding.localAddress}")
        }
    )

    val https = IO(
      config.sslContext.foreach { sslContext =>
        Http()
          .newServerAt(config.collectorConfig.interface, config.collectorConfig.ssl.port)
          .enableHttps(ConnectionContext.httpsServer(sslContext))
          .bind(routes)
          .foreach { binding =>
            logger.info(s"HTTPS REST interface bound to ${binding.localAddress}")
          }
      }
    )

  }.as(ExitCode.Success)
}
