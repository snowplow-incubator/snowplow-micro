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

import com.snowplowanalytics.snowplow.collectors.scalastream.sinks.Sink
import com.snowplowanalytics.snowplow.enrich.common.loaders.ThriftLoader
import com.snowplowanalytics.snowplow.enrich.common.adapters.{
  AdapterRegistry,
  RawEvent
}
import com.snowplowanalytics.snowplow.enrich.common.utils.JsonUtils
import com.snowplowanalytics.snowplow.enrich.common.utils.ConversionUtils.decodeBase64Url
import com.snowplowanalytics.iglu.client.Resolver
import com.snowplowanalytics.iglu.client.validation.ValidatableJsonMethods._
import org.slf4j.LoggerFactory
import scalaz.{Validation, Success, Failure}
import com.fasterxml.jackson.databind.JsonNode
import scala.collection.JavaConverters._

/** Sink of the collector that Snowplow Micro is.
  * Contain the functions that are called for each event sent
  * to the collector endpoint.
  * For each event received, it tries to validate it using scala-common-enrich,
  * and then stores the results in memory in [[ValidationCache]].
  * The events are received as [[CollectorPayload]]s serialized with Thrift.
  */
private[micro] final case class MemorySink(resolver: Resolver) extends Sink {
  implicit val resolv = resolver

  val MaxBytes = Long.MaxValue

  /** Main function of a [[Sink]], called for all the events received by a collector. */
  override def storeRawEvents(events: List[Array[Byte]], key: String) = {
    events.foreach(bytes => processThriftBytes(bytes, resolver))
    Nil
  }

  /** Deserialize Thrift bytes into [[CollectorPayload]]s,
    * validate them and store the result in [[ValidationCache]].
    * A [[CollectorPayload]] can contain several events.
    */
  private[micro] def processThriftBytes(
    thriftBytes: Array[Byte],
    resolver: Resolver
  ): Unit =
    ThriftLoader.toCollectorPayload(thriftBytes) match {
      case Success(maybePayload) =>
        maybePayload match {
          case Some(collectorPayload) =>
            AdapterRegistry.toRawEvents(collectorPayload) match {
              case Success(rawEvents) =>
                val validationResults = rawEvents.list.map(extractEventInfo)
                val good = validationResults.collect { case Success(goodEvent) => goodEvent }
                val bad = validationResults.collect { case Failure(error) =>
                  BadEvent(
                    Some(collectorPayload),
                    List("The event has been successfully validated but an error internal to Snowplow Micro occured.", error)
                  )
                }
                ValidationCache.addToGood(good)
                ValidationCache.addToBad(bad)
              case Failure(errors) =>
                val bad = BadEvent(Some(collectorPayload), List("Error while extracting event(s) from collector payload and validating it/them.") ++ errors.list)
                ValidationCache.addToBad(List(bad))
            }
          case None =>
            val bad = BadEvent(None, List("No payload."))
            ValidationCache.addToBad(List(bad))
        }
      case Failure(errors) =>
        val bad = BadEvent(None, List("Can't deserialize Thrift bytes.") ++ errors.list)
        ValidationCache.addToBad(List(bad))
    }

  /** Extract the event type if any, the schema if any, and the schemas of the contexts if any from the event.
    * @return [[GoodEvent]] with the extracted event type, schema and contexts,
    *   or error message if an error occured while retrieving one of them.
    */
  private[micro] def extractEventInfo(event: RawEvent): Validation[String, GoodEvent] =
    getEventSchema(event) match {
      case Success(schema) =>
        getEventContexts(event).bimap(
          contextsError => s"Error while extracting the contexts of the event: $contextsError",
          contexts => GoodEvent(event, getEventType(event), schema, contexts)
        )
      case Failure(errorSchema) => Failure(s"Error while extracting the schema of the event: $errorSchema") 
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
  private[micro] def getEventSchema(event: RawEvent): Validation[String, Option[String]] =
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
                decodeBase64Url("", base64_sdj)
                  .flatMap(extractDataFromSDJ)
                  .flatMap(extractSchemaFromSDJ)
                  .map(Some(_))
              case None => Failure("event_type is \"ue\" but neither \"ue_pr\" nor \"ue_px\" is set")
            }
          }
      case _ => Success(None)
    }

  /** Extract the schema of a self-describing JSON.
    * @param sdj Self-describing JSON.
    * @return [[Success]] with the Iglu schema
    *   or [[Failure]] with the error message if there was a problem.
    */
  private[micro] def extractSchemaFromSDJ(sdj: JsonNode): Validation[String, String] =
    sdj.validateAndIdentifySchema(true).bimap(
      errors => errors.list.map(_.getMessage()).mkString("; "),
      schemaAndJson => schemaAndJson._1.toString()
    )

  /** Extract the schema of a self-describing JSON.
    * @param sdj Self-describing JSON.
    * @return [[Success]] with the Iglu schema
    *   or [[Failure]] with the error message if there was a problem.
    */
  private[micro] def extractSchemaFromSDJ(sdj: String): Validation[String, String] =
    JsonUtils.extractJson("", sdj)
      .flatMap(extractSchemaFromSDJ)

  /** Extract the data of a self-describing JSON.
    * @param sdj Self-describing JSON.
    * @return [[Success]] with the content of the "data" field
    *   or [[Failure]] with the error message if there was a problem.
    */
  private[micro] def extractDataFromSDJ(sdj: String): Validation[String, JsonNode] =
    JsonUtils.extractJson("", sdj)
      .flatMap(extractDataFromSDJ)

  /** Extract the data of a self-describing JSON.
    * @param sdj Self-describing JSON.
    * @return [[Success]] with the content of the "data" field
    *   or [[Failure]] with the error message if there was a problem.
    */
  private[micro] def extractDataFromSDJ(sdj: JsonNode): Validation[String, JsonNode] =
    sdj.validate(true)
      .leftMap(errors => errors.list.map(_.getMessage).mkString("; "))

  /** Extract the schemas of the contexts of the event, if any.
    * @return - List with Iglu schemas of the contexts of the event, if any.
    *   - [[None]] if the event doesn't contain any context (neither "co" nor "cx" is set).
    *   - Error message if the event has contexts but there was a problem while reading their schema.
    */
  private[micro] def getEventContexts(event: RawEvent): Validation[String, Option[List[String]]] =
    (event.parameters.get("co") match {
      case Some(context) =>
        JsonUtils.extractJson("", context)
          .map(Some(_))
      case None =>
        event.parameters.get("cx") match {
          case Some(base64_context) =>
            decodeBase64Url("", base64_context)
              .flatMap(jsonStr => JsonUtils.extractJson("", jsonStr))
              .map(Some(_))
          case None => Success(None)
        }
    }).flatMap { // map on the Option[JsonNode]
      case Some(jsonWithContexts) =>
        extractDataFromSDJ(jsonWithContexts)
          .flatMap { jsonNode =>
            val contextsSchemas = jsonNode.elements().asScala.toList
              .map(extractSchemaFromSDJ)
            contextsSchemas.collect {
              case Failure(error) => error
            } match {
              case List() => // no error while getting all the schemas of the context
                Success(Some(contextsSchemas.collect {
                  case Success(schema) => schema
                }))
              case errors =>
                Failure(errors.mkString("; "))
            }
          }
        case None => Success(None)
    }
}
