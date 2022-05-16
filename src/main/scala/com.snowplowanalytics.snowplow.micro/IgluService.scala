/*
 * Copyright (c) 2019-2022 Snowplow Analytics Ltd. All rights reserved.
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

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes.NotFound
import cats.Id
import io.circe.Json
import io.circe.generic.auto._

import com.snowplowanalytics.iglu.client.Client
import com.snowplowanalytics.iglu.core.{SchemaVer, SchemaKey}

import IdImplicits._
import CirceSupport._

class IgluService(client: Client[Id, Json]) {

  def get(vendor: String, name: String, versionStr: String): Route =
    SchemaVer.parseFull(versionStr) match {
      case Right(version) =>
        val key = SchemaKey(vendor, name, "jsonschema", version)
        client.resolver.lookupSchema(key) match {
          case Right(json) => complete(json)
          case Left(error) => complete(NotFound, error)
        }
      case Left(_) => reject
  }

}
