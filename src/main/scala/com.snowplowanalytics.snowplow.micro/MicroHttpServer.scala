package com.snowplowanalytics.snowplow.micro

import cats.effect.{IO, Resource}
import cats.implicits._
import com.avast.datadog4s.api.Tag
import com.avast.datadog4s.extension.http4s.DatadogMetricsOps
import com.avast.datadog4s.{StatsDMetricFactory, StatsDMetricFactoryConfig}
import com.snowplowanalytics.snowplow.collector.core.{Config => CollectorConfig}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.headers.`Strict-Transport-Security`
import org.http4s.server.Server
import org.http4s.server.middleware.{HSTS, Metrics}
import org.http4s.{HttpApp, HttpRoutes}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.InetSocketAddress
import javax.net.ssl.SSLContext


/** The same http server builder as in collector but with option to customize SSLContext */
object MicroHttpServer {

  implicit private def logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def build(routes: HttpRoutes[IO],
            port: Int,
            secure: Boolean,
            customSslContext: Option[SSLContext],
            hsts: CollectorConfig.HSTS,
            networking: CollectorConfig.Networking,
            metricsCollectorConfig: CollectorConfig.Metrics
           ): Resource[IO, Server] =
    for {
      withMetricsMiddleware <- createMetricsMiddleware(routes, metricsCollectorConfig)
      server <- buildBlazeServer(withMetricsMiddleware, port, secure, customSslContext, hsts, networking)
    } yield server

  private def createMetricsMiddleware(
                                       routes: HttpRoutes[IO],
                                       metricsCollectorConfig: CollectorConfig.Metrics
                                     ): Resource[IO, HttpRoutes[IO]] =
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

  private def buildBlazeServer(routes: HttpRoutes[IO],
                               port: Int,
                               secure: Boolean,
                               customSslContext: Option[SSLContext],
                               hsts: CollectorConfig.HSTS,
                               networking: CollectorConfig.Networking
                              ): Resource[IO, Server] =
    Resource.eval(Logger[IO].info("Building blaze server")) >>
      BlazeServerBuilder[IO]
        .bindSocketAddress(new InetSocketAddress(port))
        .withHttpApp(hstsMiddleware(hsts, routes.orNotFound))
        .withIdleTimeout(networking.idleTimeout)
        .withMaxConnections(networking.maxConnections)
        .cond(secure, _.withSslContext(customSslContext.getOrElse(SSLContext.getDefault)))
        .resource

  implicit class ConditionalAction[A](item: A) {
    def cond(cond: Boolean, action: A => A): A =
      if (cond) action(item) else item
  }
}
