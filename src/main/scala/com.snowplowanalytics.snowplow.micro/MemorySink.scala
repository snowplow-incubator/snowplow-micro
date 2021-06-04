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

import cats.implicits._
import cats.Id
import cats.data.Validated
import cats.effect.Clock

import io.circe.Json

import java.util.concurrent.TimeUnit

import org.joda.time.DateTime

import com.snowplowanalytics.iglu.client.Client
import com.snowplowanalytics.iglu.client.resolver.registries.RegistryLookup

import com.snowplowanalytics.snowplow.analytics.scalasdk.{Event, EventConverter}
import com.snowplowanalytics.snowplow.badrows.{BadRow, Payload}
import com.snowplowanalytics.snowplow.collectors.scalastream.sinks.Sink
import com.snowplowanalytics.snowplow.enrich.common.adapters.{AdapterRegistry, RawEvent}
import com.snowplowanalytics.snowplow.enrich.common.enrichments.{EnrichmentManager, EnrichmentRegistry}
import com.snowplowanalytics.snowplow.enrich.common.loaders.ThriftLoader
import com.snowplowanalytics.snowplow.enrich.common.utils.ConversionUtils

import com.snowplowanalytics.snowplow.badrows.Processor

/** Sink of the collector that Snowplow Micro is.
  * Contains the functions that are called for each tracking event sent
  * to the collector endpoint.
  * The events are received as `CollectorPayload`s serialized with Thrift.
  * For each event it tries to validate it using Common Enrich,
  * and then stores the results in-memory in [[ValidationCache]].
  */
private[micro] final case class MemorySink(igluClient: Client[Id, Json]) extends Sink {
  val MaxBytes = Int.MaxValue
  private val enrichmentRegistry = new EnrichmentRegistry[Id]()
  private val processor = Processor(buildinfo.BuildInfo.name, buildinfo.BuildInfo.version)

  implicit val clockProvider: Clock[Id] = new Clock[Id] {
    final def realTime(unit: TimeUnit): Id[Long] =
      unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    final def monotonic(unit: TimeUnit): Id[Long] =
      unit.convert(System.nanoTime(), TimeUnit.NANOSECONDS)
  }

  implicit val registryLookup: RegistryLookup[Id] = StaticRegistryLookup

  /** Function of the [[Sink]] called for all the events received by a collector. */
  override def storeRawEvents(events: List[Array[Byte]], key: String) = {
    events.foreach(bytes => processThriftBytes(bytes, igluClient, enrichmentRegistry, processor))
    Nil
  }

  /** Deserialize Thrift bytes into `CollectorPayload`s,
    * validate them and store the result in [[ValidationCache]].
    * A `CollectorPayload` can contain several events.
    */
  private[micro] def processThriftBytes(
    thriftBytes: Array[Byte],
    igluClient: Client[Id, Json],
    enrichmentRegistry: EnrichmentRegistry[Id],
    processor: Processor
  ): Unit =
    ThriftLoader.toCollectorPayload(thriftBytes, processor) match {
      case Validated.Valid(maybePayload) =>
        maybePayload match {
          case Some(collectorPayload) =>
            new AdapterRegistry().toRawEvents(collectorPayload, igluClient, processor) match {
              case Validated.Valid(rawEvents) =>
                val (goodEvents, badEvents) = rawEvents.toList.foldRight((Nil, Nil) : (List[GoodEvent], List[BadEvent])) {
                  case (rawEvent, (good, bad)) =>
                    validateEvent(rawEvent, igluClient, enrichmentRegistry, processor) match {
                      case Right(goodEvent) =>
                        (goodEvent :: good, bad)
                      case Left(errors) =>
                        val badEvent = 
                        BadEvent(
                          Some(collectorPayload),
                          Some(rawEvent),
                          errors
                        )
                        (good, badEvent :: bad)
                    }
                }
                ValidationCache.addToGood(goodEvents)
                ValidationCache.addToBad(badEvents)
              case Validated.Invalid(badRow) =>
                val bad = BadEvent(Some(collectorPayload), None, List("Error while extracting event(s) from collector payload and validating it/them.", badRow.toString()))
                ValidationCache.addToBad(List(bad))
            }
          case None =>
            val bad = BadEvent(None, None, List("No payload."))
            ValidationCache.addToBad(List(bad))
        }
      case Validated.Invalid(badRows) =>
        val bad = BadEvent(None, None, List("Can't deserialize Thrift bytes.") ++ badRows.toList.map(_.compact))
        ValidationCache.addToBad(List(bad))
    }

  /** Validate the raw event using Common Enrich logic, and extract the event type if any,
    * the schema if any, and the schemas of the contexts attached to the event if any.
    * @return [[GoodEvent]] with the extracted event type, schema and contexts,
    *   or error if the event couldn't be validated.
    */
  private[micro] def validateEvent(
    rawEvent: RawEvent,
    igluClient: Client[Id, Json],
    enrichmentRegistry: EnrichmentRegistry[Id],
    processor: Processor
  ): Either[List[String], GoodEvent] =
    EnrichmentManager.enrichEvent[Id](enrichmentRegistry, igluClient, processor, DateTime.now(), rawEvent)
      .subflatMap { enriched =>
        EventConverter.fromEnriched(enriched)
          .leftMap { failure =>
            BadRow.LoaderParsingError(processor, failure, Payload.RawPayload(ConversionUtils.tabSeparatedEnrichedEvent(enriched)))
          }
          .toEither
      }
      .value.bimap(
      badRow => List("Error while validating the event", badRow.compact),
      enriched => GoodEvent(rawEvent, enriched.event, getEnrichedSchema(enriched), getEnrichedContexts(enriched), enriched)
    )

  private[micro] def getEnrichedSchema(enriched: Event): Option[String] = 
    List(enriched.event_vendor, enriched.event_name, enriched.event_format, enriched.event_version)
      .sequence
      .map(_.mkString("iglu:", "/", ""))

  private[micro] def getEnrichedContexts(enriched: Event): List[String] =
    enriched.contexts.data.map(_.schema.toSchemaUri)

}
