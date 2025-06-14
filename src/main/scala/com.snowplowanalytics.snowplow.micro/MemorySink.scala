/*
 * Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.1
 * located at https://docs.snowplow.io/limited-use-license-1.1
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
 * OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
 */

package com.snowplowanalytics.snowplow.micro

import cats.data.{NonEmptyList, Validated}
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
import com.snowplowanalytics.snowplow.enrich.common.enrichments.{EnrichmentManager, EnrichmentRegistry}
import com.snowplowanalytics.snowplow.enrich.common.loaders.ThriftLoader
import com.snowplowanalytics.snowplow.enrich.common.utils.{ConversionUtils, OptionIor, OptionIorT}
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import com.snowplowanalytics.snowplow.micro.Configuration.EnrichConfig

import java.time.Instant

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
                       enrichConfig: EnrichConfig) extends Sink[IO] {
  override val maxBytes = Int.MaxValue
  private lazy val logger = LoggerFactory.getLogger("EventLog")

  private val adapterRegistry = new AdapterRegistry[IO](Map.empty, enrichConfig.adaptersSchemas)

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
    ThriftLoader.toCollectorPayload(thriftBytes, processor, Instant.now()) match {
      case Validated.Valid(collectorPayload) =>
        adapterRegistry.toRawEvents(collectorPayload, igluClient, processor, registryLookup, enrichConfig.maxJsonDepth, Instant.now()).flatMap {
          case Validated.Valid(rawEvents) =>
            val partitionEvents = rawEvents.toList.foldLeftM((Nil, Nil): (List[GoodEvent], List[BadEvent])) {
              case ((good, bad), rawEvent) =>
                validateEvent(rawEvent).value.map {
                  case OptionIor.Right(goodEvent) =>
                    logger.info(s"GOOD ${formatEvent(goodEvent)}")
                    (goodEvent :: good, bad)
                  case OptionIor.Both((errors, badRow), _) =>
                    val badEvent = BadEvent(Some(collectorPayload), Some(rawEvent), errors)
                    logger.warn(s"BAD ${formatBadRow(badRow)}")
                    (good, badEvent :: bad)
                  case OptionIor.Left((errors, badRow)) =>
                    val badEvent = BadEvent(Some(collectorPayload), Some(rawEvent), errors)
                    logger.warn(s"BAD ${formatBadRow(badRow)}")
                    (good, badEvent :: bad)
                  case OptionIor.None =>
                    (good, bad)
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
  private[micro] def validateEvent(rawEvent: RawEvent): OptionIorT[IO, (List[String], BadRow), GoodEvent] =
    EnrichmentManager.enrichEvent[IO](
        enrichmentRegistry,
        igluClient,
        processor,
        DateTime.now(),
        rawEvent,
        EtlPipeline.FeatureFlags(acceptInvalid = false),
        IO.unit,
        registryLookup,
        enrichConfig.validation.atomicFieldsLimits,
        emitIncomplete = false,
        enrichConfig.maxJsonDepth
      )
      .leftMap(NonEmptyList.one(_)) // Because the following `.flatMap requires a SemiGroup on the Left
      .flatMap { enriched =>
        val ior: OptionIor[NonEmptyList[BadRow], Event] = EventConverter.fromEnriched(enriched) match {
          case Validated.Valid(converted) =>
            OptionIor.Right(converted)
          case Validated.Invalid(failure) =>
            val badRow = BadRow.LoaderParsingError(processor, failure, Payload.RawPayload(ConversionUtils.tabSeparatedEnrichedEvent(enriched)))
            OptionIor.Left(NonEmptyList.one(badRow))
        }
        OptionIorT(IO.pure(ior))
      }
      .map { enriched =>
        GoodEvent(rawEvent, enriched.event, getEnrichedSchema(enriched), getEnrichedContexts(enriched), enriched)
      }
      .leftMap { nel =>
        // We can ignore the .tail of this non-emoty-list because we only expect a single bad row
        val badRow = nel.head
        (List("Error while validating the event.", badRow.compact), badRow)
      }

  private def getEnrichedSchema(enriched: Event): Option[String] =
    List(enriched.event_vendor, enriched.event_name, enriched.event_format, enriched.event_version)
      .sequence
      .map(_.mkString("iglu:", "/", ""))

  private def getEnrichedContexts(enriched: Event): List[String] =
    enriched.contexts.data.map(_.schema.toSchemaUri)

}
