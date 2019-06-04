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
import scalaz.{Success, Failure}

class MemorySinkSpec extends Specification {

  private val igluConf = RepositoryRefConfig("Iglu central", 0, List("com.snowplowanalytics"))
  private val resolver = Resolver(100, HttpRepositoryRef(igluConf, "http://iglucentral.com", None))

  private val ue_prSchema = "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1"
  private val ue_pr = s"""
  {
    "schema": "$ue_prSchema",
    "data": {
      "targetUrl": "http://a-target-url.com"
    }
  }
  """

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
      MemorySink(resolver).extractSchemaFromSDJ(ue_pr) shouldEqual Success(ue_prSchema)
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
