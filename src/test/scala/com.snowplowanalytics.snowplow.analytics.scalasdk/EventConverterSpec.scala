/*
 * Copyright (c) 2019-2021 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.analytics.scalasdk

import org.specs2.mutable.Specification
import java.time.Instant
import io.circe.Json

import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.snowplow.enrich.common.outputs.EnrichedEvent

class EventConverterSpec extends Specification {
  import EventConverterSpec._

  "the event converter" >> {
    "should convert between between valid events" >> {
      val enriched = newValidEvent
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.v_etl must_== enriched.v_etl
        e.v_collector must_== enriched.v_collector
      }
    }

    "should convert to a ParsingError if the event is missing required fields" >> {
      val enriched = newValidEvent
      enriched.v_collector = ""
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beLeft
    }

    "should convert to a ParsingError if the event has an invalid timestamp" >> {
      val enriched = newValidEvent
      enriched.collector_tstamp = "xxxx"
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beLeft
    }

    "should correctly convert timestamp fields" >> {
      val enriched = newValidEvent
      enriched.collector_tstamp = "2020-05-05 05:05:05"
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.collector_tstamp must_== Instant.parse("2020-05-05T05:05:05Z")
      }
    }

    "should correctly convert optional timestamp fields" >> {
      val enriched = newValidEvent
      enriched.etl_tstamp = "2020-05-05 05:05:05"
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.etl_tstamp must beSome { t: Instant =>
          t must_== Instant.parse("2020-05-05T05:05:05Z")
        }
      }
    }

    "should correctly convert uuid fields" >> {
      val enriched = newValidEvent
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.event_id.toString() must_== enriched.event_id
      }
    }

    "should correctly convert optional int fields" >> {
      val enriched = newValidEvent
      enriched.ti_quantity = 42
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.ti_quantity must beSome(42)
      }
    }

    "should correctly convert missing optional int fields" >> {
      val enriched = newValidEvent
      enriched.ti_quantity = null
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.ti_quantity must beNone
      }
    }

    "should correctly convert missing string to optional int fields" >> {
      val enriched = newValidEvent
      enriched.txn_id = null
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.txn_id must beNone
      }
    }

    "should correctly convert optional string fields" >> {
      val enriched = newValidEvent
      enriched.name_tracker = "my_tracker"
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.name_tracker must_== Some("my_tracker")
      }
    }

    "should correctly convert optional double fields" >> {
      val enriched = newValidEvent
      enriched.geo_latitude = 42.123f
      val tolerance = 0.00001
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.geo_latitude must beSome { d: Double =>
          d must beBetween(42.123 - tolerance, 42.123 + tolerance)
        }
      }
    }

    "should correctly convert truthy optional boolean fields" >> {
      val enriched = newValidEvent
      enriched.br_features_pdf = 1.toByte
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.br_features_pdf must_== Some(true)
      }
    }

    "should correctly convert falsy optional boolean fields" >> {
      val enriched = newValidEvent
      enriched.br_features_pdf = 0.toByte
      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.br_features_pdf must_== Some(false)
      }
    }

    "should correctly convert contexts" >> {
      val enriched = newValidEvent
      enriched.contexts = """{
        "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-0",
        "data": [
          {
            "schema": "iglu:org.schema/WebPage/jsonschema/1-0-0",
            "data": {
              "genre": "blog",
              "inLanguage": "en-US",
              "datePublished": "2014-11-06T00:00:00Z",
              "author": "Fred Blundun",
              "breadcrumb": [
                "blog",
                "releases"
              ],
              "keywords": [
                "snowplow",
                "javascript",
                "tracker",
                "event"
              ]
            }
          }
        ]
      }"""

      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.contexts.data must have size (1)
        e.contexts.data must contain { context: SelfDescribingData[Json] =>
          context.schema.vendor must_== "org.schema"
          context.schema.name must_== "WebPage"
        }
      }
    }

    "should correctly convert unstruct events" >> {
      val enriched = newValidEvent
      enriched.unstruct_event = """{
          "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
          "data": {
            "schema": "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
            "data": {
              "targetUrl": "http://www.example.com",
              "elementClasses": ["foreground"],
              "elementId": "exampleLink"
            }
          }
      }"""

      val result = EventConverter.fromEnriched(enriched)
      result.toEither must beRight { e: Event =>
        e.unstruct_event.data must beSome { data: SelfDescribingData[Json] =>
          data.schema.vendor must_== "com.snowplowanalytics.snowplow"
          data.schema.name must_== "link_click"
        }
      }
    }
  }
}

object EventConverterSpec {

  def newValidEvent: EnrichedEvent = {
    val e = new EnrichedEvent()
    e.collector_tstamp = "2020-01-01 11:11:11"
    e.event_id = "97b3b199-bc24-45e1-8f88-0860598bf86e"
    e.v_collector = "my_collector"
    e.v_etl = "my_etl"
    e
  }
}
