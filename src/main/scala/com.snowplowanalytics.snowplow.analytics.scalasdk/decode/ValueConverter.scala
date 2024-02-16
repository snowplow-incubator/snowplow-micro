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
    ofFunc(key => x => decoder.parse(key, x.getOrElse(""), None))

  implicit def floatDoubleCase: Aux[Option[Float], Option[Double]] =
    simple(_.map(_.toDouble))

  implicit def intCase: Aux[Option[Int], Option[Int]] =
    simple(identity)

  implicit def byteBoolCase: Aux[Option[Byte], Option[Boolean]] =
    simple(_.map(byte => byte > 0))

}
