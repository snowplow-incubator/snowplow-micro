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

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.testing.specs2.CatsEffect
import io.circe.Json
import org.http4s.Method.GET
import org.http4s.Request
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterEach

import scala.concurrent.duration.{Duration, DurationInt}

class MicroApiSpec extends Specification with CatsEffect with BeforeAfterEach {
  sequential

  override protected val Timeout: Duration = 1.minute 

  override protected def before: Unit = ValidationCache.reset()

  override protected def after: Unit = ValidationCache.reset()

  "Micro should accept good data" in {
    setup().use { client =>
      for {
        _ <- client.run(Request(GET, uri"http://localhost:9090/i?e=pp&p=web&tv=lol")).use_
        all <- client.run(Request(GET, uri"http://localhost:9090/micro/all")).use(_.as[Json])
        good <- client.run(Request(GET, uri"http://localhost:9090/micro/good")).use(_.as[Json])
      } yield {

        all.noSpaces must beEqualTo("""{"total":1,"good":1,"bad":0}""")
        good.isArray must beTrue
        good.asArray.map(_.length).get must beEqualTo(1)

        val firstEvent = good.hcursor.downN(0)
        firstEvent.downField("eventType").as[String] must beRight("page_ping")
        firstEvent.downField("schema").as[String] must beRight("iglu:com.snowplowanalytics.snowplow/page_ping/jsonschema/1-0-0")
        firstEvent.downField("event").downField("platform").as[String] must beRight("web")
        firstEvent.downField("event").downField("v_tracker").as[String] must beRight("lol")
      }
    }
  }

  "Micro should handle bad data" in {
    setup().use { client =>
      for {
        _ <- client.run(Request(GET, uri"http://localhost:9090/i?e=pp&p=web&tv=lol&eid=invalidEventId")).use_
        all <- client.run(Request(GET, uri"http://localhost:9090/micro/all")).use(_.as[Json])
        bad <- client.run(Request(GET, uri"http://localhost:9090/micro/bad")).use(_.as[Json])
      } yield {

        bad.isArray must beTrue
        bad.asArray.map(_.length).get must beEqualTo(1)
        all.noSpaces must beEqualTo("""{"total":1,"good":0,"bad":1}""")
      }
    }
  }

  "Micro should reset stored data" in {
    setup().use { client =>
      for {
        _ <- client.run(Request(GET, uri"http://localhost:9090/i?e=pp&p=web&tv=lol")).use_
        _ <- client.run(Request(GET, uri"http://localhost:9090/i?e=pp&p=web&tv=lol&eid=invalidEventId")).use_
        beforeReset <- client.run(Request(GET, uri"http://localhost:9090/micro/all")).use(_.as[Json])
        afterReset <- client.run(Request(GET, uri"http://localhost:9090/micro/reset")).use(_.as[Json])
      } yield {

        beforeReset.noSpaces must beEqualTo("""{"total":2,"good":1,"bad":1}""")
        afterReset.noSpaces must beEqualTo("""{"total":0,"good":0,"bad":0}""")
      }
    }
  }

  private def setup(): Resource[IO, Client[IO]] = {
    for {
      _ <- Main.run(List.empty).background
      client <- BlazeClientBuilder.apply[IO].resource
      _ <- Resource.sleep[IO](1.seconds)
    } yield client
  }
}