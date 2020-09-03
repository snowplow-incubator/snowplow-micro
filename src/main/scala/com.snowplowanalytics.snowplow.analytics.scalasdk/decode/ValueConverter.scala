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

import cats.syntax.either._

/** Type class to convert a key-value pair.
  *  The key corresponds to the field name of a case class.
  *  In and Out correspond to the field value types in the case classes which are being converted.
  */
trait ValueConverter[In] {
  type Result
  def apply(key: Symbol, x: In): DecodedValue[Result]
}

object ValueConverter {
  def apply[In, Result](implicit myCase: Aux[In, Result]): Aux[In, Result] = myCase

  type Aux[In, Result0] = ValueConverter[In] { type Result = Result0 }

  def ofFunc[A, B](f: Symbol => A => DecodedValue[B]): Aux[A, B] =
    new ValueConverter[A] {
      type Result = B
      def apply(key: Symbol, x: A): DecodedValue[B] =
        f(key)(x)
    }

  def simple[A, B](f: A => B): Aux[A, B] =
    ofFunc(_ => f(_).asRight)

  implicit def valueDecoderCase[A](implicit decoder: ValueDecoder[A]): Aux[Option[String], A] =
    ofFunc(key => x => decoder.parse(key, x.getOrElse("")))

  implicit def floatDoubleCase: Aux[Option[Float], Option[Double]] =
    simple(_.map(_.toDouble))

  implicit def intCase: Aux[Option[Int], Option[Int]] =
    simple(identity)

  implicit def byteBoolCase: Aux[Option[Byte], Option[Boolean]] =
    simple(_.map(byte => byte > 0))

}
