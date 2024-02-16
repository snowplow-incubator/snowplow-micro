/*
 * Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This software is made available by Snowplow Analytics, Ltd.,
 * under the terms of the Snowplow Limited Use License Agreement, Version 1.0
 * located at https://docs.snowplow.io/limited-use-license-1.0
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
 * OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
 */

package com.snowplowanalytics.snowplow.analytics.scalasdk.decode

import shapeless._
import shapeless.LabelledGeneric

import com.snowplowanalytics.snowplow.analytics.scalasdk.ParsingError

/** Converts a case class A into a different case class B.
  *  The two case classes must have identical field names.
  *  Each field must be convertible via a [[ValueConverter]].
  */
object GenericConverter {
  def convert[A, B, ARepr <: HList, BRepr <: HList](a: A)(implicit
    aGen: LabelledGeneric.Aux[A, ARepr],
    bGen: LabelledGeneric.Aux[B, BRepr],
    conv: RowConverter.Aux[ARepr, BRepr]
  ): DecodeResult[B] =
    conv.apply(aGen.to(a)).bimap(ParsingError.RowDecodingError(_), bGen.from(_))

}
