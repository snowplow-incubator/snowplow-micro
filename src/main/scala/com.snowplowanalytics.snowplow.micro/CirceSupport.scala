/*
 * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
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

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, ContentTypeRange, HttpEntity}
import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import io.circe.{Decoder, Encoder, Json, Printer, jawn}
import scala.collection.immutable.Seq
import org.joda.time.DateTime
import org.apache.http.NameValuePair

/** Add support for unmarshalling HTTP JSON requests
  * and marshalling HTTP JSON responses, using circe library.
  * More information about marshalling can be found here
  * https://doc.akka.io/docs/akka-http/current/common/marshalling.html.
  * 
  * This code mostly comes from https://github.com/hseeberger/akka-http-json.
  */
private[micro] object CirceSupport {

  // To encode the datetime in a CollectorPayload
  implicit val dateTimeEncoder: Encoder[DateTime] =
    Encoder[String].contramap(_.toString)

  // To encode the querystring in a CollectorPayload
  implicit val nameValuePairEncoder: Encoder[NameValuePair] =
    Encoder[String].contramap(kv => s"${kv.getName()}=${kv.getValue()}")

  def unmarshallerContentTypes: Seq[ContentTypeRange] =
    mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] =
    List(`application/json`)

  /** `Json` => HTTP entity
    * @return marshaller for JSON value
    */
  implicit final def jsonMarshaller(
      implicit printer: Printer = Printer.noSpaces
  ): ToEntityMarshaller[Json] =
    Marshaller.oneOf(mediaTypes: _*) { mediaType =>
      Marshaller.withFixedContentType(ContentType(mediaType)) { json =>
        HttpEntity(
          mediaType,
          ByteString(
            printer.prettyByteBuffer(json, mediaType.charset.nioCharset())))
      }
    }

  /** `A` => HTTP entity
    * @tparam A type to encode
    * @return marshaller for any `A` value
    */
  implicit final def marshaller[A: Encoder](
      implicit printer: Printer = Printer.noSpaces
  ): ToEntityMarshaller[A] =
    jsonMarshaller(printer).compose(Encoder[A].apply)

  /** HTTP entity => `Json`
    * @return unmarshaller for `Json`
    */
  implicit final val jsonUnmarshaller: FromEntityUnmarshaller[Json] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .map {
        case ByteString.empty => throw Unmarshaller.NoContentException
        case data =>
          jawn.parseByteBuffer(data.asByteBuffer).fold(throw _, identity)
      }

  /** HTTP entity => `A`
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */
  implicit def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    def decode(json: Json) = Decoder[A].decodeJson(json).fold(throw _, identity)
    jsonUnmarshaller.map(decode)
  }
}
