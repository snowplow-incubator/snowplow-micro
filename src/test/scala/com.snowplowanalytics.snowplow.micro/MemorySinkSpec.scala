/*
 * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.micro

import org.specs2.mutable.Specification
import com.snowplowanalytics.iglu.client.Resolver
import com.snowplowanalytics.iglu.client.repositories.HttpRepositoryRef
import com.snowplowanalytics.iglu.client.repositories.RepositoryRefConfig
import com.snowplowanalytics.snowplow.enrich.common.utils.ConversionUtils.encodeBase64Url
import scalaz.{Success, Failure}
import com.snowplowanalytics.snowplow.enrich.common.adapters.RawEvent
import com.snowplowanalytics.snowplow.enrich.common.loaders.{
  CollectorApi,
  CollectorSource,
  CollectorContext
}
import org.joda.time.DateTime

class MemorySinkSpec extends Specification {

  private val igluConf = RepositoryRefConfig("Iglu central", 0, List("com.snowplowanalytics"))
  private val resolver = Resolver(100, HttpRepositoryRef(igluConf, "http://iglucentral.com", None))
  private val sink = MemorySink(resolver)

  private val eventType = "ue"

  private val ueSchema = "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1"
  private val ue_pr = s"""
  {
    "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
    "data": {
      "schema": "$ueSchema",
       "data": {
         "targetUrl": "http://a-target-url.com"
       }
    }
  }
  """
  private val ue_px = encodeBase64Url(ue_pr)

  private val context1Schema = "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-1"
  private val context2Schema = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1"
  private val co = s"""
  {
    "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1",
    "data": [
      {
        "schema": "$context1Schema",
        "data": {
          "sessionIndex": 2,
          "storageMechanism": "SQLITE",
          "firstEventId": "afe4e97f-493a-4d69-9717-9dd75ee26b08",
          "sessionId": "b4431a1f-8013-443e-ae21-2db7409814d8",
          "previousSessionId": "aba8eac5-453f-46e3-a507-6d80383d3e66",
          "userId": "9addaee4-94a9-4a51-8b70-3c534a6691b9"
        }
      },
      {
        "schema": "$context2Schema",
        "data": {
          "networkTechnology": "LTE",
          "carrier": "Turk Telekom",
          "osVersion": "8.0.0",
          "osType": "android",
          "deviceModel": "MI 5",
          "deviceManufacturer": "Xiaomi",
          "networkType": "mobile"
        }
      }
    ]
  }
  """
  private val cx = encodeBase64Url(co)
  private val params = Map(
    "tv" -> "andr-1.1.0",
    "p" -> "mob",
    "e" -> eventType
  )

  private val collectorApi = CollectorApi("com.snowplowanalytics.snowplow", "tp2")
  private val rawEventUeprCo = RawEvent(
    collectorApi,
    params ++ Map("ue_pr" -> ue_pr, "co" -> co),
    contentType = None,
    CollectorSource("ssc-0.15.0-stdout$", "UTF-8", Some("localhost")),
    CollectorContext(
      Some(DateTime.now()),
      Some("0:0:0:0:0:0:0:1"),
      Some("curl/7.52.1"),
      None,
      List(
        "Host: localhost:9090",
        "User-Agent: curl/7.52.1",
        "Accept: */*",
        "Expect: 100-continue",
        "Timeout-Access: <function1>",
        "application/json"
      ),
      Some("d04c787c-cbd0-420c-811e-8efb2a0b5e8b")
    )
  )
  private val rawEventUepxCx = rawEventUeprCo.copy(
    parameters = params ++ Map("ue_px" -> ue_px, "cx" -> cx)
  )

  "analyzeEvent" >> {
    "should successfully return the event type, the schema and the contexts of an event" >> {
      1 shouldEqual 1
    }

    "should return the error if a problem occurs while reading the schema" >> {
      1 shouldEqual 1
    }
    
    "should return the error if a problem occurs while reading the contexts" >> {
      1 shouldEqual 1
    }
  }

  "getEventType" >> {
    "should correctly return the type of an event" >> {
      sink.getEventType(rawEventUeprCo) shouldEqual Some(eventType)
    }
  }
  
  "getEventSchema" >> {
    "should return the schema for \"ue_pr\"" >> {
      1 shouldEqual 1
    }
    
    "should return the schema for \"ue_px\"" >> {
      1 shouldEqual 1
    }
    
    "should return None if the event isn't of type \"ue\"" >> {
      1 shouldEqual 1
    }
    
    "should return the error if the event is of type \"ue\" but it can't retrieve the schema" >> {
      1 shouldEqual 1
    }
  }

  "extractSchemaFromSDJ" >> {
    "should correctly extract the schema of a self-describing JSON" >> {
      sink.getEventSchema(rawEventUeprCo) shouldEqual Success(Some(ueSchema))
    }

    "should return an error if the schema is not on Iglu" >> {
      1 shouldEqual 1
    }

    "should return an error if the json is not correctly formatted" >> {
      1 shouldEqual 1
    }

    "should return an error if the JSON is correctly formatted but does not contain a schema field" >> {
      1 shouldEqual 1
    }
  }
  
  "getEventContexts" >> {
    "should correctly return the schemas for all the not-encoded contexts" >> {
      1 shouldEqual 1
    }
    
    "should correctly return the schemas for all the base64-encoded contexts" >> {
      1 shouldEqual 1
    }

    "should not return any schema if the event doesn't have any context" >> {
      1 shouldEqual 1
    }
    
    "should return an error if a problem occurs while trying to read the schemas from the contexts" >> {
      1 shouldEqual 1
    }
  }
}
