package com.snowplowanalytics.snowplow.micro

import cats.effect.{IO, Ref}
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event
import com.snowplowanalytics.snowplow.badrows.BadRow

class InMemoryStore(good: Ref[IO, List[Event]],
                    bad: Ref[IO, List[BadRow]]) {

  def addGood(event: Event): IO[Unit] = {
    good.update(_ :+ event)
  }

  def addBad(badrow: BadRow): IO[Unit] = {
    bad.update(_ :+ badrow)
  }

}

object InMemoryStore {
  def mk(): IO[InMemoryStore] =
    for {
      good <- Ref.of[IO, List[Event]](List.empty)
      bad <- Ref.of[IO, List[BadRow]](List.empty)
    } yield new InMemoryStore(good, bad)
}
