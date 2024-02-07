package com.snowplowanalytics.snowplow.micro

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object Main
  extends CommandIOApp(
    name = buildinfo.BuildInfo.version,
    header = "Something",
    version = buildinfo.BuildInfo.version) {

  override def main: Opts[IO[ExitCode]] = Micro.run()
}