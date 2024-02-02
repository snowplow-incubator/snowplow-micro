package com.snowplowanalytics.snowplow.micro

import cats.effect.IO
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.badrows.BadRow
import com.snowplowanalytics.snowplow.micro.Micro.Streams
import io.circe.parser.decode

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object Processing {

  def run(streams: Streams,
          store: InMemoryStore): IO[Unit] = {
    processingEnrichedOutput(streams, store)
      .merge(processingBadOutput(streams, store))
      .compile
      .drain
  }

  private def processingEnrichedOutput(streams: Streams,
                                       store: InMemoryStore): fs2.Stream[IO, Unit] = {
    streams.enriched.read
      .map(parseGood)
      .evalTap(store.addGood)
      .evalMap(event => IO(println(s"ENRICHED - [${event.toTsv}]")))
  }

  private def processingBadOutput(streams: Streams,
                                  store: InMemoryStore): fs2.Stream[IO, Unit] = {
    streams.bad.read
      .map(parseBadRow)
      .evalTap(store.addBad)
      .evalMap(badRow => IO(println(s"BAD - [${badRow.compact}]")))
  }

  private def parseGood(bytes: Array[Byte]): Event = {
    Event.parseBytes(ByteBuffer.wrap(bytes))
      .getOrElse(throw new RuntimeException("Imposssibru"))
  }

  private def parseBadRow(bytes: Array[Byte]): BadRow = {
    decode[SelfDescribingData[BadRow]](new String(bytes, StandardCharsets.UTF_8))
      .getOrElse(throw new RuntimeException("Imposssibru"))
      .data
  }
}
