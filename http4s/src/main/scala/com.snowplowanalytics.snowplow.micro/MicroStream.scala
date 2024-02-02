package com.snowplowanalytics.snowplow.micro

import cats.effect.std.Queue
import cats.effect.{IO, Resource}

trait MicroStream {
  def sink(data: Array[Byte]): IO[Unit]
  def read: fs2.Stream[IO, Array[Byte]]
}

object MicroStream {
  def inMemory: Resource[IO, MicroStream] = {
    Resource.eval(Queue.unbounded[IO, Array[Byte]]).map { queue => 
      new MicroStream {
        override def sink(data: Array[Byte]): IO[Unit] = queue.offer(data) 
        override def read: fs2.Stream[IO, Array[Byte]] =
          fs2.Stream.fromQueueUnterminated(queue)
      }
    }
  }
}
