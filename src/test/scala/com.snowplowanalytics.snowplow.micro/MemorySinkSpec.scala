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

import cats.effect.testing.specs2.CatsResource
import cats.effect.{IO, Resource}
import com.snowplowanalytics.iglu.client.IgluCirceClient
import com.snowplowanalytics.iglu.client.resolver.Resolver
import com.snowplowanalytics.iglu.client.resolver.registries.{JavaNetRegistryLookup, Registry}
import com.snowplowanalytics.snowplow.badrows.Processor
import com.snowplowanalytics.snowplow.enrich.common.enrichments.EnrichmentRegistry
import org.specs2.mutable.SpecificationLike

class MemorySinkSpec extends CatsResource[IO, MemorySink] with SpecificationLike {

  import events._

  override val resource: Resource[IO, MemorySink] = Resource.eval(createSink())

  sequential

  "processThriftBytes" >> {
    "should add a BadEvent to the cache if the array of bytes is not a valid Thrift payload" >> withResource { sink =>
      ValidationCache.reset()
      val bytes = Array(1, 3, 5, 7).map(_.toByte)
      sink.processThriftBytes(bytes).map { _ =>
        ValidationCache.filterBad() must beLike { case List(badEvent) if badEvent.errors.exists(_.contains("Can't deserialize Thrift bytes")) => ok }
        ValidationCache.filterGood().size must beEqualTo(0)
      }
    }

    "should add a BadEvent to the cache if RawEvent(s) can't be extracted from the CollectorPayload" >> withResource { sink =>
      ValidationCache.reset()
      val bytes = buildThriftBytesBadCollectorPayload()
      sink.processThriftBytes(bytes).map { _ =>
        ValidationCache.filterBad() must beLike { case List(badEvent) if badEvent.errors.exists(_.contains("Error while extracting event(s) from collector payload")) => ok }
        ValidationCache.filterGood().size must beEqualTo(0)
      }
    }

    "should add a GoodEvent and a BadEvent to the cache for a CollectorPayload containing both" >> withResource { sink =>
      ValidationCache.reset()
      val bytes = buildThriftBytes1Good1Bad()
      sink.processThriftBytes(bytes).map { _ =>
        ValidationCache.filterBad() must beLike { case List(badEvent) if badEvent.errors.exists(_.contains("Error while validating the event")) => ok }
        ValidationCache.filterGood().size must beEqualTo(1)
      }
    }
  }

  "validateEvent" >> {
    "should fail if the timestamp is not valid" >> withResource { sink =>
      val raw = buildRawEvent()
      val withoutTimestamp = raw.copy(context = raw.context.copy(timestamp = None))
      val expected = "Error while validating the event"
      sink.validateEvent(withoutTimestamp).value.map { result =>
        result must beLeft.like {
          case (errors, _) if errors.exists(_.contains(expected)) => ok
          case errs => ko(s"$errs doesn't contain [$expected]")
        }
      }
    }

    "should fail if the event type parameter is not set" >> withResource { sink =>
      val raw = buildRawEvent()
      val withoutEvent = raw.copy(parameters = raw.parameters - "e")
      val expected = "Error while validating the event"
      sink.validateEvent(withoutEvent).value.map { result =>
        result must beLeft.like {
          case (errors, _) if errors.exists(_.contains(expected)) => ok
          case errs => ko(s"$errs doesn't contain [$expected]")
        }
      }
    }

    "should fail for an invalid unstructured event" >> withResource { sink =>
      val raw = buildRawEvent(Some(buildUnstruct(sdjInvalid)))
      val expected = "Error while validating the event"
      sink.validateEvent(raw).value.map { result =>
        result must beLeft.like {
          case (errors, _) if errors.exists(_.contains(expected)) => ok
          case errs => ko(s"$errs doesn't contain [$expected]")
        }
      }
    }

    "should fail if the event has an invalid context" >> withResource { sink =>
      val raw = buildRawEvent(None, Some(buildContexts(List(sdjInvalid))))
      val expected = "Error while validating the event"
      sink.validateEvent(raw).value.map { result =>
        result must beLeft.like {
          case (errors, _) if errors.exists(_.contains(expected)) => ok
          case errs => ko(s"$errs doesn't contain [$expected]")
        }
      }
    }

    "should fail for a unstructured event with an unknown schema" >> withResource { sink =>
      val raw = buildRawEvent(Some(buildUnstruct(sdjDoesNotExist)))
      val expected = "Error while validating the event"
      sink.validateEvent(raw).value.map { result =>
        result must beLeft.like {
          case (errors, _) if errors.exists(_.contains(expected)) => ok
          case errs => ko(s"$errs doesn't contain [$expected]")
        }
      }
    }

    "should fail if the event has a context with an unknown schema" >> withResource { sink =>
      val raw = buildRawEvent(None, Some(buildContexts(List(sdjDoesNotExist))))
      val expected = "Error while validating the event"
      sink.validateEvent(raw).value.map { result =>
        result must beLeft.like {
          case (errors, _) if errors.exists(_.contains(expected)) => ok
          case errs => ko(s"$errs doesn't contain [$expected]")
        }
      }
    }

    "extract the type of an event" >> withResource { sink =>
      val raw = buildRawEvent()
      val expected = "page_ping"
      sink.validateEvent(raw).value.map { result =>
        result must beRight.like {
          case GoodEvent(_, typE, _, _, _) if typE == Some(expected) => ok
          case GoodEvent(_, typE, _, _, _) => ko(s"extracted type $typE isn't $expected")
        }
      }
    }

    "should extract the schema of an unstructured event" >> withResource { sink =>
      val raw = buildRawEvent(Some(buildUnstruct(sdjLinkClick)))
      val expected = schemaLinkClick
      sink.validateEvent(raw).value.map { result =>
        result must beRight.like {
          case GoodEvent(_, _, schema, _, _) if schema == Some(expected) => ok
          case GoodEvent(_, _, schema, _, _) => ko(s"extracted schema $schema isn't $expected")
        }
      }
    }

    "should extract the contexts of an event" >> withResource { sink =>
      val raw = buildRawEvent(None, Some(buildContexts(List(sdjLinkClick, sdjMobileContext))))
      val expected = List(schemaLinkClick, schemaMobileContext)
      sink.validateEvent(raw).value.map { result =>
        result must beRight.like {
          case GoodEvent(_, _, _, contexts, _) if contexts == expected => ok
          case GoodEvent(_, _, _, contexts, _) => ko(s"extracted contexts $contexts isn't $expected")
        }
      }
    }
  }

  private def createSink(): IO[MemorySink] = {
    for {
      enrichConfig <- Configuration.loadEnrichConfig().value.map(_.getOrElse(throw new IllegalArgumentException("Can't read defaults from Enrich config")))
      igluClient <- IgluCirceClient.fromResolver[IO](Resolver[IO](List(Registry.IgluCentral), None), 500, enrichConfig.maxJsonDepth)
      enrichmentRegistry = new EnrichmentRegistry[IO]()
      processor = Processor(BuildInfo.name, BuildInfo.version)
      lookup = JavaNetRegistryLookup.ioLookupInstance[IO]
    } yield new MemorySink(igluClient, lookup, enrichmentRegistry, false, processor, enrichConfig)
  }

}
