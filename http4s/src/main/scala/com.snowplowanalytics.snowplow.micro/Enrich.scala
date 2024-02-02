package com.snowplowanalytics.snowplow.micro

import cats.effect.{IO, Resource, ResourceIO}
import cats.implicits.toTraverseOps
import com.snowplowanalytics.snowplow.enrich.common.fs2.config.{CliConfig => EnrichConfig}
import com.snowplowanalytics.snowplow.enrich.common.fs2.{AttributedByteSink, ByteSink, RunEnrich}
import com.snowplowanalytics.snowplow.micro.ResourceUtils.resolveConfig

import java.nio.file.{Path, Paths}

object Enrich {

  def run(cliConfig: Cli.Config,
          streams: Micro.Streams): ResourceIO[Unit] = {
    for {
      config <- resolveEnrichConfig(cliConfig)
      _ <- RunEnrich.run[IO, Array[Byte]](
        name = buildinfo.BuildInfo.name,
        version = buildinfo.BuildInfo.version,
        cfg = config,
        mkSource = (_, _) => streams.raw.read,
        mkSinkGood = _ => Resource.pure(mkAttributedSink(streams.enriched)),
        mkSinkPii = _ => Resource.pure(mkAttributedSink(streams.pii)),
        mkSinkBad = _ => Resource.pure(mkByteSink(streams.bad)),
        checkpoint = _ => IO.unit,
        mkClients = _ => List.empty,
        getPayload = identity,
        maxRecordSize = Int.MaxValue,
        cloud = None,
        getRegion = None
      ).background
    } yield ()
  }

  private def resolveEnrichConfig(cliConfig: Cli.Config): Resource[IO, EnrichConfig] = {
    for {
      appConfig <- resolveConfig(cliConfig.enrich, "enrich.conf")
      igluConfig <- resolveConfig(cliConfig.iglu, "default-iglu-resolver.json")
      enrichmentsConfig <- readEnrichments()
    } yield {
      EnrichConfig(Right(appConfig), Right(igluConfig), Right(enrichmentsConfig))
    }
  }

  private def readEnrichments(): Resource[IO, Path] = {
    Option(getClass.getResource("/enrichments")) match {
      case Some(definedEnrichments) => Resource.pure(Paths.get(definedEnrichments.toURI))
      case None => Resource.pure(Paths.get("."))
    }
  }

  private def mkAttributedSink(microStream: MicroStream): AttributedByteSink[IO] = records =>
    records.traverse(record => microStream.sink(record.data)).void

  private def mkByteSink(microStream: MicroStream): ByteSink[IO] = records =>
    records.traverse(record => microStream.sink(record)).void

}
