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
