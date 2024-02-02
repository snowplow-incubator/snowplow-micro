package com.snowplowanalytics.snowplow.micro

import cats.effect.{ExitCode, IO, Resource}
import com.monovore.decline.Opts

object Micro {

  final case class Streams(raw: MicroStream,
                           enriched: MicroStream,
                           bad: MicroStream,
                           pii: MicroStream)

  def run(): Opts[IO[ExitCode]] = {
    Cli.config.map { cliConfig =>
      val resource = for {
        streams <- mkStreams()
        inMemoryStore <- Resource.eval(InMemoryStore.mk())
        _ <- Collector.run(cliConfig, streams)
        _ <- Enrich.run(cliConfig, streams)
        _ <- Processing.run(streams, inMemoryStore).background
      } yield ExitCode.Success

      resource.use { exitCode => IO.never.as(exitCode) }
    }
  }

  private def mkStreams(): Resource[IO, Streams] = {
    for {
      raw <- MicroStream.inMemory
      enriched <- MicroStream.inMemory
      bad <- MicroStream.inMemory
      pii <- MicroStream.inMemory
    } yield Streams(raw, bad, enriched, pii)
  }
}
