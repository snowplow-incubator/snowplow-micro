package com.snowplowanalytics.snowplow.micro

import cats.effect.{IO, Resource}
import fs2.io.file.Files

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.io.Source

object ResourceUtils {
  
  def resolveConfig(custom: Option[Path], fallbackResource: String): Resource[IO, Path] =
    custom match {
      case Some(definedCustomPath) => Resource.pure(definedCustomPath)
      case None => loadResourceToTemporaryPath(fallbackResource)
    }

  private def loadResourceToTemporaryPath(resource: String): Resource[IO, Path] = {
    for {
      source <- Resource.make(IO(Source.fromResource(resource)))(source => IO(source.close()))
      tempConfigFile <- Files[IO].tempFile
      _ <- Resource.eval(fs2.Stream.emits(source.mkString.getBytes(StandardCharsets.UTF_8))
        .through(Files[IO].writeAll(tempConfigFile)).compile.drain)
    } yield tempConfigFile.toNioPath
  }
}
