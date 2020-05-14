/*
 * Copyright (c) 2019-2020 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.snowplow.collectors.scalastream.sinks.Sink
import com.snowplowanalytics.snowplow.enrich.common.loaders.ThriftLoader
import com.snowplowanalytics.snowplow.enrich.common.adapters.{
  AdapterRegistry,
  RawEvent
}
import com.snowplowanalytics.snowplow.enrich.common.utils.JsonUtils
import com.snowplowanalytics.snowplow.enrich.common.utils.ConversionUtils.decodeBase64Url
import com.snowplowanalytics.snowplow.badrows.Processor
import com.snowplowanalytics.iglu.client.Client
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.instances._
import cats.implicits._
import cats.Id
import cats.data.Validated
import cats.effect.Clock
import io.circe.Json
import java.util.concurrent.TimeUnit

/** Sink of the collector that Snowplow Micro is.
  * Contains the functions that are called for each event sent
  * to the collector endpoint.
  * For each event received, it tries to validate it using scala-common-enrich,
  * and then stores the results in memory in [[ValidationCache]].
  * The events are received as `CollectorPayload`s serialized with Thrift.
  */
private[micro] final case class MemorySink(igluClient: Client[Id, Json]) extends Sink {
  val MaxBytes = Int.MaxValue

  implicit val clockProvider: Clock[Id] = new Clock[Id] {
    final def realTime(unit: TimeUnit): Id[Long] =
      unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    final def monotonic(unit: TimeUnit): Id[Long] =
      unit.convert(System.nanoTime(), TimeUnit.NANOSECONDS)
  }

  /** Function of the [[Sink]] called for all the events received by a collector. */
  override def storeRawEvents(events: List[Array[Byte]], key: String) = {
    events.foreach(bytes => processThriftBytes(bytes, igluClient))
    Nil
  }

  /** Deserialize Thrift bytes into [[CollectorPayload]]s,
    * validate them and store the result in [[ValidationCache]].
    * A [[CollectorPayload]] can contain several events.
    */
  private[micro] def processThriftBytes(
    thriftBytes: Array[Byte],
    igluClient: Client[Id, Json],
    processor: Processor = Processor("snowplow", "micro")
  ): Unit =
    ThriftLoader.toCollectorPayload(thriftBytes, processor) match {
      case Validated.Valid(maybePayload) =>
        maybePayload match {
          case Some(collectorPayload) =>
            new AdapterRegistry().toRawEvents(collectorPayload, igluClient, processor) match {
              case Validated.Valid(rawEvents) =>
                val validationResults = rawEvents.map(extractEventInfo)
                val good = validationResults.collect { case Right(goodEvent) => goodEvent }
                val bad = validationResults.collect { case Left(error) =>
                  BadEvent(
                    Some(collectorPayload),
                    List("An error occured while extracting the info about the event.", error)
                  )
                }
                ValidationCache.addToGood(good)
                ValidationCache.addToBad(bad)
              case Validated.Invalid(badRow) =>
                val bad = BadEvent(Some(collectorPayload), List("Error while extracting event(s) from collector payload.") ++ List(badRow.toString()))
                ValidationCache.addToBad(List(bad))
            }
          case None =>
            val bad = BadEvent(None, List("No payload."))
            ValidationCache.addToBad(List(bad))
        }
      case Validated.Invalid(errors) =>
        val bad = BadEvent(None, List("Can't deserialize Thrift bytes.") ++ errors.map(_.toString()).toList)
        ValidationCache.addToBad(List(bad))
    }

  /** Extract the event type if any, the schema if any, and the schemas of the contexts if any from the event.
    * @return [[GoodEvent]] with the extracted event type, schema and contexts,
    *   or error message if an error occured while retrieving one of them.
    */
  private[micro] def extractEventInfo(event: RawEvent): Either[String, GoodEvent] =
    getEventSchema(event) match {
      case Right(schema) =>
        getEventContexts(event).bimap(
          contextsError => s"Error while extracting the contexts of the event: $contextsError",
          contexts => GoodEvent(event, getEventType(event), schema, contexts)
        )
      case Left(errorSchema) => Left(s"Error while extracting the schema of the event: $errorSchema") 
    }

  /** Extract the event type of an event if any (in param "e"). */
  private[micro] def getEventType(event: RawEvent): Option[String] =
    event.parameters.get("e")

  /** Extract the schema of a custom unstructured event (type "ue").
    * @return - [[None]] if the event is not of type "ue".
    *   - Iglu schema if the event is of type "ue" and it could successfully
    *     retrieve the schema in "ue_pr" or "ue_px".
    *   - Error message if the event is of type "ue" and an error occured
    *     while retrieving its schema.
    */
  private[micro] def getEventSchema(event: RawEvent): Either[String, Option[String]] =
    event.parameters.get("e") match {
      case Some("ue") =>
        event.parameters.get("ue_pr") match {
          case Some(json) =>
            extractDataFromSDJ(json)
              .flatMap(extractSchemaFromSDJ)
              .map(Some(_))
          case None =>
            event.parameters.get("ue_px") match {
              case Some(base64_sdj) =>
                decodeBase64Url(base64_sdj) match {
                  case Left(err) => Left(err)
                  case Right(sdj) =>
                    extractDataFromSDJ(sdj)
                      .flatMap(extractSchemaFromSDJ)
                      .map(Some(_))
                }
              case None => Left("event_type is \"ue\" but neither \"ue_pr\" nor \"ue_px\" is set")
            }
          }
      case _ => Right(None)
    }

  private[micro] def stringToSDJ(sdjStr: String): Either[String, SelfDescribingData[Json]] =
    JsonUtils.extractJson(sdjStr).flatMap { json =>
      SelfDescribingData.parse(json).leftMap(_.toString())
    }

  private[micro] def extractSchemaFromSDJ(sdj: String): Either[String, String] =
    stringToSDJ(sdj).map(_.schema.toSchemaUri)

  private[micro] def extractSchemaFromSDJ(json: Json): Either[String, String] =
    SelfDescribingData.parse(json).bimap(
      _.toString(),
      sdj => sdj.schema.toSchemaUri
    )

  private[micro] def extractDataFromSDJ(sdj: String): Either[String, Json] =
    stringToSDJ(sdj).map(_.data)

  private[micro] def extractDataFromSDJ(json: Json): Either[String, Json] =
    SelfDescribingData.parse(json).bimap(
      _.toString(),
      sdj => sdj.data
    )

  /** Extract the schemas of the contexts of the event, if any.
    * @return - List with Iglu schemas of the contexts of the event, if any.
    *   - [[None]] if the event doesn't contain any context (neither "co" nor "cx" is set).
    *   - Error message if the event has contexts but there was a problem while reading their schema.
    */
  private[micro] def getEventContexts(event: RawEvent): Either[String, Option[List[String]]] =
    event.parameters.get("co") match {
      case Some(context) =>
        JsonUtils.extractJson(context).map { json =>
          Some(extractContexts(json))
        }
      case None =>
        event.parameters.get("cx") match {
          case Some(base64_context) =>
            decodeBase64Url(base64_context)
              .flatMap(JsonUtils.extractJson)
              .map(extractContexts)
              .map(Some(_))
          case None => Right(None)
        }
    }
  
  private[micro] def extractContexts(json: Json): List[String] =
    json
      .asArray
      .get
      .toList
      .map(json => SelfDescribingData.parse(json))
      .collect { case Right(sdj) => sdj.schema.toSchemaUri }
}
