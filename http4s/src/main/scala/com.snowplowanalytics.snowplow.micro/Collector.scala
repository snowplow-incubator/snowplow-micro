package com.snowplowanalytics.snowplow.micro

import cats.effect._
import cats.implicits.toTraverseOps
import com.snowplowanalytics.snowplow.collector.core.model.Sinks
import com.snowplowanalytics.snowplow.collector.core.{AppInfo, Sink, Telemetry, Run => RunCollector}
import com.snowplowanalytics.snowplow.micro.ResourceUtils.resolveConfig
import com.snowplowanalytics.snowplow.scalatracker.emitters.http4s.ceTracking
import io.circe.Decoder

object Collector {

  private val info = new AppInfo {
    override val name: String = buildinfo.BuildInfo.name
    override val moduleName: String = buildinfo.BuildInfo.name
    override val version: String = buildinfo.BuildInfo.version
    override val dockerAlias: String = "something"
    override val shortName: String = "something"
  }

  def run(cliConfig: Cli.Config,
          streams: Micro.Streams): ResourceIO[Unit] = {
    for {
      config <- resolveConfig(cliConfig.collector, "collector.conf")
      _ <- RunCollector.fromPath[IO, DummySinkConfig](
        info,
        _ => Resource.pure(Sinks(mkSink(streams.raw), mkSink(streams.bad))),
        _ => IO.pure(Telemetry.TelemetryInfo(None, None, None)),
        Some(config)
      ).background
    } yield ()
  }

  final case class DummySinkConfig()

  implicit val decoder: Decoder[DummySinkConfig] = Decoder.instance(_ => Right(DummySinkConfig()))

  private def mkSink(stream: MicroStream): Sink[IO] = new Sink[IO] {
    override val maxBytes: Int = Int.MaxValue

    override def isHealthy: IO[Boolean] = IO.pure(true)

    override def storeRawEvents(events: List[Array[Byte]], key: String): IO[Unit] = {
      events.traverse(payload => stream.sink(payload)).void
    }
  }
}
