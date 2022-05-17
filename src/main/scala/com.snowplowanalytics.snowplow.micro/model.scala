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

import com.snowplowanalytics.snowplow.enrich.common.adapters.RawEvent
import com.snowplowanalytics.snowplow.enrich.common.loaders.CollectorPayload
import com.snowplowanalytics.snowplow.analytics.scalasdk.Event

/** A list of this case class is returned when /micro/good is queried. */
private [micro] final case class GoodEvent(
  rawEvent: RawEvent,
  eventType: Option[String],
  schema: Option[String],
  contexts: List[String],
  event: Event
)

/** A list of this case class is returned when /micro/bad is queried. */
private [micro] final case class BadEvent(
  collectorPayload: Option[CollectorPayload],
  rawEvent: Option[RawEvent],
  errors: List[String]
)

/** Format of the JSON with the filters for a request made to /micro/good. */
private [micro] final case class FiltersGood(
  event_type: Option[String],
  schema: Option[String],
  contexts: Option[List[String]],
  limit: Option[Int]
)

/** Format of the JSON with the filters for a request made to /micro/bad. */
private [micro] final case class FiltersBad(
  vendor: Option[String],
  version: Option[String],
  limit: Option[Int]
)

/** Retuned when /micro/all is queried, and also /micro/reset. */
private [micro] final case class ValidationSummary(total: Int, good: Int, bad: Int)
