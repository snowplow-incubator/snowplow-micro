/*
 * Copyright (c) 2019-2021 Snowplow Analytics Ltd. All rights reserved.
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

import cats.Id
import io.circe.parser
import io.circe.Json
import scala.io.Source
import scala.util.control.NonFatal

import com.snowplowanalytics.iglu.client.resolver.registries.{Registry, RegistryError, RegistryLookup}
import com.snowplowanalytics.iglu.core.{SchemaKey, SchemaList}



object StaticRegistryLookup extends RegistryLookup[Id] {

  private val fallback: RegistryLookup[Id] = RegistryLookup.idLookupInstance

  override def lookup(registry: Registry, schemaKey: SchemaKey): Either[RegistryError, Json] = {
    try {
      val file = s"/schemas/${schemaKey.toPath}"
      Right(parser.parse(Source.fromFile(file).mkString).right.get)
    } catch { case NonFatal(_) =>
      fallback.lookup(registry, schemaKey)
    }
  }


  override def list(
    registry: Registry,
    vendor: String,
    name: String,
    model: Int): Either[RegistryError, SchemaList] =
      fallback.list(registry, vendor, name, model)
}
