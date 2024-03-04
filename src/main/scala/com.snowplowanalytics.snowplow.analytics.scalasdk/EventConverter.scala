/*
 * Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.0
 * located at https://docs.snowplow.io/limited-use-license-1.0
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
 * OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
 */

package com.snowplowanalytics.snowplow.analytics.scalasdk

import com.snowplowanalytics.snowplow.analytics.scalasdk.decode.{DecodeResult, GenericConverter}
import com.snowplowanalytics.snowplow.enrich.common.outputs.EnrichedEvent
import com.snowplowanalytics.snowplow.badrows.Payload.PartiallyEnrichedEvent

/** Converts between different representations of a Snopwlow Event
  */
object EventConverter {

  def fromPartiallyEnriched(e: PartiallyEnrichedEvent): DecodeResult[Event] =
    GenericConverter.convert(e)

  def fromEnriched(e: EnrichedEvent): DecodeResult[Event] =
    fromPartiallyEnriched(EnrichedEvent.toPartiallyEnrichedEvent(e))

}
