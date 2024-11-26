/*
 * Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.0
 * located at https://docs.snowplow.io/limited-use-license-1.0
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
 * OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
 */

package com.snowplowanalytics.snowplow.micro

import cats.effect.{IO, Resource}
import com.avast.datadog4s.api.Tag
import com.avast.datadog4s.extension.http4s.DatadogMetricsOps
import com.avast.datadog4s.{StatsDMetricFactory, StatsDMetricFactoryConfig}
import com.snowplowanalytics.snowplow.collector.core.{Config => CollectorConfig}
import com.snowplowanalytics.snowplow.micro.Configuration.{MicroConfig, SinkConfig}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.headers.`Strict-Transport-Security`
import org.http4s.server.Server
import org.http4s.server.middleware.{HSTS, Metrics, Timeout}
import org.http4s.{HttpApp, HttpRoutes}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetSocketAddress
import javax.net.ssl.SSLContext


/** Similar to HTTP server builder as in collector but with option to customize SSLContext */
object MicroHttpServer {

  implicit private def logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def build(routes: HttpRoutes[IO],
            config: MicroConfig,
            sslContext: Option[SSLContext]): Resource[IO, Unit] =
    for {
      withMetricsMiddleware <- createMetricsMiddleware(routes, config.collector.monitoring.metrics)
      _ <- Resource.eval(Logger[IO].info("Building blaze server"))
      _ <- buildHTTPServer(withMetricsMiddleware, config)
      _ <- sslContext.map(definedSSL => buildHTTPSServer(withMetricsMiddleware, config, definedSSL)).getOrElse(Resource.unit[IO])
    } yield ()

  private def buildHTTPServer(routes: HttpRoutes[IO], config: MicroConfig): Resource[IO, Server] =
    builder(routes, config.collector)
      .bindSocketAddress(new InetSocketAddress(config.collector.port))
      .resource

  private def buildHTTPSServer(routes: HttpRoutes[IO], config: MicroConfig, sslContext: SSLContext): Resource[IO, Server] =
    builder(routes, config.collector)
      .bindSocketAddress(new InetSocketAddress(config.collector.ssl.port))
      .withSslContext(sslContext)
      .resource

  private def builder(routes: HttpRoutes[IO], config: CollectorConfig[SinkConfig]): BlazeServerBuilder[IO] = {
    BlazeServerBuilder[IO]
      .withHttpApp(timeoutMiddleware(hstsMiddleware(config.hsts, routes.orNotFound), config.networking))
      .withIdleTimeout(config.networking.idleTimeout)
      .withMaxConnections(config.networking.maxConnections)
      .withResponseHeaderTimeout(config.networking.responseHeaderTimeout)
      .withLengthLimits(
        maxRequestLineLen = config.networking.maxRequestLineLength,
        maxHeadersLen = config.networking.maxHeadersLength
      )
  }

  private def timeoutMiddleware(routes: HttpApp[IO], networking: CollectorConfig.Networking): HttpApp[IO] =
    Timeout.httpApp[IO](timeout = networking.responseHeaderTimeout)(routes)


  private def createMetricsMiddleware(routes: HttpRoutes[IO],
                                      metricsCollectorConfig: CollectorConfig.Metrics): Resource[IO, HttpRoutes[IO]] =
    if (metricsCollectorConfig.statsd.enabled) {
      val metricsFactory = StatsDMetricFactory.make[IO](createStatsdCollectorConfig(metricsCollectorConfig))
      metricsFactory.evalMap(DatadogMetricsOps.builder[IO](_).useDistributionBasedTimers().build()).map { metricsOps =>
        Metrics[IO](metricsOps)(routes)
      }
    } else {
      Resource.pure(routes)
    }

  private def createStatsdCollectorConfig(metricsCollectorConfig: CollectorConfig.Metrics): StatsDMetricFactoryConfig = {
    val server = InetSocketAddress.createUnresolved(metricsCollectorConfig.statsd.hostname, metricsCollectorConfig.statsd.port)
    val tags = metricsCollectorConfig.statsd.tags.toVector.map { case (name, value) => Tag.of(name, value) }
    StatsDMetricFactoryConfig(Some(metricsCollectorConfig.statsd.prefix), server, defaultTags = tags)
  }

  private def hstsMiddleware(hsts: CollectorConfig.HSTS, routes: HttpApp[IO]): HttpApp[IO] =
    if (hsts.enable)
      HSTS(routes, `Strict-Transport-Security`.unsafeFromDuration(hsts.maxAge))
    else routes
}
