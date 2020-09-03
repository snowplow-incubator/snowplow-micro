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
package com.snowplowanalytics.snowplow.analytics.scalasdk.decode

import shapeless._
import shapeless.labelled.FieldType
import shapeless.labelled.field

import cats.data.Validated
import cats.syntax.either._
import cats.syntax.apply._

/** Type class to convert between the fields two compatible labelled generic representations.
  *  Each represenation (In and Out) is forced to have identical labels;
  *  i.e. identical field names in the corresponding case classes.
  */
trait RowConverter[In <: HList] {
  type Out <: HList
  def apply(in: In): RowDecodeResult[Out]
}

object RowConverter {
  def apply[L <: HList](implicit conv: RowConverter[L]): Aux[L, conv.Out] = conv

  type Aux[In <: HList, Out0 <: HList] = RowConverter[In] { type Out = Out0 }

  implicit val hnilConverter: Aux[HNil, HNil] =
    new RowConverter[HNil] {
      type Out = HNil
      def apply(l: HNil): RowDecodeResult[Out] = Validated.Valid(l)
    }

  implicit def hlistConverter[K <: Symbol, InH, InT <: HList, OutT <: HList, Result](implicit
    witness: Witness.Aux[K],
    hc: ValueConverter.Aux[InH, Result],
    mt: RowConverter.Aux[InT, OutT]
  ): Aux[FieldType[K, InH] :: InT, FieldType[K, Result] :: OutT] =
    new RowConverter[FieldType[K, InH] :: InT] {
      type Out = FieldType[K, hc.Result] :: OutT
      def apply(l: FieldType[K, InH] :: InT): RowDecodeResult[Out] = {
        val hv = hc(witness.value, l.head).toValidatedNel
        val tv = mt(l.tail)
        (hv, tv).mapN(field[K](_) :: _)
      }
    }
}
