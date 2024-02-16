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

import cats.data.{EitherT, Validated}
import cats.effect.IO
import cats.implicits._
import com.snowplowanalytics.iglu.client.IgluCirceClient
import com.snowplowanalytics.iglu.client.resolver.registries.RegistryLookup
import com.snowplowanalytics.snowplow.analytics.scalasdk.{Event, EventConverter}
import com.snowplowanalytics.snowplow.badrows.BadRow.{EnrichmentFailures, SchemaViolations, TrackerProtocolViolations}
import com.snowplowanalytics.snowplow.badrows.{BadRow, Failure, Payload, Processor}
import com.snowplowanalytics.snowplow.collector.core.Sink
import com.snowplowanalytics.snowplow.enrich.common.EtlPipeline
import com.snowplowanalytics.snowplow.enrich.common.adapters.{AdapterRegistry, RawEvent}
import com.snowplowanalytics.snowplow.enrich.common.enrichments.{AtomicFields, EnrichmentManager, EnrichmentRegistry}
import com.snowplowanalytics.snowplow.enrich.common.loaders.ThriftLoader
import com.snowplowanalytics.snowplow.enrich.common.utils.ConversionUtils
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/** Sink of the collector that Snowplow Micro is.
 * Contains the functions that are called for each tracking event sent
 * to the collector endpoint.
 * The events are received as `CollectorPayload`s serialized with Thrift.
 * For each event it tries to validate it using Common Enrich,
 * and then stores the results in-memory in [[ValidationCache]].
 */
final class MemorySink(igluClient: IgluCirceClient[IO],
                       registryLookup: RegistryLookup[IO],
                       enrichmentRegistry: EnrichmentRegistry[IO],
                       outputEnrichedTsv: Boolean,
                       processor: Processor,
                       adapterRegistry: AdapterRegistry[IO]) extends Sink[IO] {
  override val maxBytes = Int.MaxValue
  private lazy val logger = LoggerFactory.getLogger("EventLog")

  override def isHealthy: IO[Boolean] = IO.pure(true)

  override def storeRawEvents(events: List[Array[Byte]], key: String): IO[Unit] = {
    events.traverse(bytes => processThriftBytes(bytes)).void
  }

  private def formatEvent(event: GoodEvent): String =
    s"id:${event.event.event_id}" +
      event.event.app_id.fold("")(i => s" app_id:$i") +
      event.eventType.fold("")(t => s" type:$t") +
      event.schema.fold("")(s => s" ($s)")

  private def formatBadRow(badRow: BadRow): String = badRow match {
    case TrackerProtocolViolations(_, Failure.TrackerProtocolViolations(_, _, _, messages), _) =>
      messages.map(_.asJson).toList.mkString
    case SchemaViolations(_, Failure.SchemaViolations(_, messages), _) =>
      messages.map(_.asJson).toList.mkString
    case EnrichmentFailures(_, Failure.EnrichmentFailures(_, messages), _) =>
      messages.map(_.message.asJson).toList.mkString
    case _ => "Error while validating the event."
  }

  /** Deserialize Thrift bytes into `CollectorPayload`s,
   * validate them and store the result in [[ValidationCache]].
   * A `CollectorPayload` can contain several events.
   */
  private[micro] def processThriftBytes(thriftBytes: Array[Byte]): IO[Unit] =
    ThriftLoader.toCollectorPayload(thriftBytes, processor) match {
      case Validated.Valid(maybePayload) =>
        maybePayload match {
          case Some(collectorPayload) =>
            adapterRegistry.toRawEvents(collectorPayload, igluClient, processor, registryLookup).flatMap {
              case Validated.Valid(rawEvents) =>
                val partitionEvents = rawEvents.toList.foldLeftM((Nil, Nil): (List[GoodEvent], List[BadEvent])) {
                  case ((good, bad), rawEvent) =>
                    validateEvent(rawEvent).value.map {
                      case Right(goodEvent) =>
                        logger.info(s"GOOD ${formatEvent(goodEvent)}")
                        (goodEvent :: good, bad)
                      case Left((errors, badRow)) =>
                        val badEvent =
                          BadEvent(
                            Some(collectorPayload),
                            Some(rawEvent),
                            errors
                          )
                        logger.warn(s"BAD ${formatBadRow(badRow)}")
                        (good, badEvent :: bad)
                    }
                }
                partitionEvents.map {
                  case (goodEvents, badEvents) =>
                    ValidationCache.addToGood(goodEvents)
                    ValidationCache.addToBad(badEvents)
                    if (outputEnrichedTsv) {
                      goodEvents.foreach { event =>
                        println(event.event.toTsv)
                      }
                    } else ()
                }
              case Validated.Invalid(badRow) =>
                val bad = BadEvent(Some(collectorPayload), None, List("Error while extracting event(s) from collector payload and validating it/them.", badRow.compact))
                logger.warn(s"BAD ${bad.errors.head}")
                IO(ValidationCache.addToBad(List(bad)))
            }
          case None =>
            val bad = BadEvent(None, None, List("No payload."))
            logger.warn(s"BAD ${bad.errors.head}")
            IO(ValidationCache.addToBad(List(bad)))
        }
      case Validated.Invalid(badRows) =>
        val bad = BadEvent(None, None, List("Can't deserialize Thrift bytes.") ++ badRows.toList.map(_.compact))
        logger.warn(s"BAD ${bad.errors.head}")
        IO(ValidationCache.addToBad(List(bad)))
    }

  /** Validate the raw event using Common Enrich logic, and extract the event type if any,
   * the schema if any, and the schemas of the contexts attached to the event if any.
   * @return [[GoodEvent]] with the extracted event type, schema and contexts,
   *   or error if the event couldn't be validated.
   */
  private[micro] def validateEvent(rawEvent: RawEvent): EitherT[IO, (List[String], BadRow), GoodEvent] =
    EnrichmentManager.enrichEvent[IO](
        enrichmentRegistry,
        igluClient,
        processor,
        DateTime.now(),
        rawEvent,
        EtlPipeline.FeatureFlags(acceptInvalid = false, legacyEnrichmentOrder = false),
        IO.unit,
        registryLookup,
        AtomicFields.from(Map.empty)
      )
      .subflatMap { enriched =>
        EventConverter.fromEnriched(enriched)
          .leftMap { failure =>
            BadRow.LoaderParsingError(processor, failure, Payload.RawPayload(ConversionUtils.tabSeparatedEnrichedEvent(enriched)))
          }
          .toEither
      }
      .bimap(
        badRow => (List("Error while validating the event.", badRow.compact), badRow),
        enriched => GoodEvent(rawEvent, enriched.event, getEnrichedSchema(enriched), getEnrichedContexts(enriched), enriched)
      )


  private def getEnrichedSchema(enriched: Event): Option[String] =
    List(enriched.event_vendor, enriched.event_name, enriched.event_format, enriched.event_version)
      .sequence
      .map(_.mkString("iglu:", "/", ""))

  private def getEnrichedContexts(enriched: Event): List[String] =
    enriched.contexts.data.map(_.schema.toSchemaUri)
}
