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

import com.snowplowanalytics.snowplow.enrich.common.outputs.EnrichedEvent

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

  // To encode null values with circe
  def handleNull[S, T](f: S => Json, vOrNull: T)(implicit convert: T => S): Json =
    Option(vOrNull) match {
      case Some(v) => f(v)
      case None => Json.Null
    }

  implicit val enrichedEventEncoder: Encoder[EnrichedEvent] =
    new Encoder[EnrichedEvent] {
      final def apply(e: EnrichedEvent): Json = Json.obj(
        ("app_id", handleNull(Json.fromString, e.app_id)),
        ("platform", handleNull(Json.fromString, e.platform)),
        ("etl_tstamp", handleNull(Json.fromString, e.etl_tstamp)),
        ("collector_tstamp", handleNull(Json.fromString, e.collector_tstamp)),
        ("dvce_created_tstamp", handleNull(Json.fromString, e.dvce_created_tstamp)),
        ("event", handleNull(Json.fromString, e.event)),
        ("event_id", handleNull(Json.fromString, e.event_id)),
        ("txn_id", handleNull(Json.fromString, e.txn_id)),
        ("name_tracker", handleNull(Json.fromString, e.name_tracker)),
        ("v_tracker", handleNull(Json.fromString, e.v_tracker)),
        ("v_collector", handleNull(Json.fromString, e.v_collector)),
        ("v_etl", handleNull(Json.fromString, e.v_etl)),
        ("user_id", handleNull(Json.fromString, e.user_id)),
        ("user_ipaddress", handleNull(Json.fromString, e.user_ipaddress)),
        ("user_fingerprint", handleNull(Json.fromString, e.user_fingerprint)),
        ("domain_userid", handleNull(Json.fromString, e.domain_userid)),
        ("domain_sessionidx", handleNull(Json.fromInt, e.domain_sessionidx)),
        ("network_userid", handleNull(Json.fromString, e.network_userid)),
        ("geo_country", handleNull(Json.fromString, e.geo_country)),
        ("geo_region", handleNull(Json.fromString, e.geo_region)),
        ("geo_city", handleNull(Json.fromString, e.geo_city)),
        ("geo_zipcode", handleNull(Json.fromString, e.geo_zipcode)),
        ("geo_latitude", handleNull(Json.fromFloatOrNull, e.geo_latitude)),
        ("geo_longitude", handleNull(Json.fromFloatOrNull, e.geo_longitude)),
        ("geo_region_name", handleNull(Json.fromString, e.geo_region_name)),
        ("ip_isp", handleNull(Json.fromString, e.ip_isp)),
        ("ip_organization", handleNull(Json.fromString, e.ip_organization)),
        ("ip_domain", handleNull(Json.fromString, e.ip_domain)),
        ("ip_netspeed", handleNull(Json.fromString, e.ip_netspeed)),
        ("page_url", handleNull(Json.fromString, e.page_url)),
        ("page_title", handleNull(Json.fromString, e.page_title)),
        ("page_referrer", handleNull(Json.fromString, e.page_referrer)),
        ("page_urlscheme", handleNull(Json.fromString, e.page_urlscheme)),
        ("page_urlhost", handleNull(Json.fromString, e.page_urlhost)),
        ("page_urlport", handleNull(Json.fromInt, e.page_urlport)),
        ("page_urlpath", handleNull(Json.fromString, e.page_urlpath)),
        ("page_urlquery", handleNull(Json.fromString, e.page_urlquery)),
        ("page_urlfragment", handleNull(Json.fromString, e.page_urlfragment)),
        ("refr_urlscheme", handleNull(Json.fromString, e.refr_urlscheme)),
        ("refr_urlhost", handleNull(Json.fromString, e.refr_urlhost)),
        ("refr_urlport", handleNull(Json.fromInt, e.refr_urlport)),
        ("refr_urlpath", handleNull(Json.fromString, e.refr_urlpath)),
        ("refr_urlquery", handleNull(Json.fromString, e.refr_urlquery)),
        ("refr_urlfragment", handleNull(Json.fromString, e.refr_urlfragment)),
        ("refr_medium", handleNull(Json.fromString, e.refr_medium)),
        ("refr_source", handleNull(Json.fromString, e.refr_source)),
        ("refr_term", handleNull(Json.fromString, e.refr_term)),
        ("mkt_medium", handleNull(Json.fromString, e.mkt_medium)),
        ("mkt_source", handleNull(Json.fromString, e.mkt_source)),
        ("mkt_term", handleNull(Json.fromString, e.mkt_term)),
        ("mkt_content", handleNull(Json.fromString, e.mkt_content)),
        ("mkt_campaign", handleNull(Json.fromString, e.mkt_campaign)),
        ("contexts", handleNull(Json.fromString, e.contexts)),
        ("se_category", handleNull(Json.fromString, e.se_category)),
        ("se_action", handleNull(Json.fromString, e.se_action)),
        ("se_label", handleNull(Json.fromString, e.se_label)),
        ("se_property", handleNull(Json.fromString, e.se_property)),
        ("se_value", handleNull(Json.fromString, e.se_value)),
        ("unstruct_event", handleNull(Json.fromString, e.unstruct_event)),
        ("tr_orderid", handleNull(Json.fromString, e.tr_orderid)),
        ("tr_affiliation", handleNull(Json.fromString, e.tr_affiliation)),
        ("tr_total", handleNull(Json.fromString, e.tr_total)),
        ("tr_tax", handleNull(Json.fromString, e.tr_tax)),
        ("tr_shipping", handleNull(Json.fromString, e.tr_shipping)),
        ("tr_city", handleNull(Json.fromString, e.tr_city)),
        ("tr_state", handleNull(Json.fromString, e.tr_state)),
        ("tr_country", handleNull(Json.fromString, e.tr_country)),
        ("ti_orderid", handleNull(Json.fromString, e.ti_orderid)),
        ("ti_sku", handleNull(Json.fromString, e.ti_sku)),
        ("ti_name", handleNull(Json.fromString, e.ti_name)),
        ("ti_category", handleNull(Json.fromString, e.ti_category)),
        ("ti_price", handleNull(Json.fromString, e.ti_price)),
        ("ti_quantity", handleNull(Json.fromInt, e.ti_quantity)),
        ("pp_xoffset_min", handleNull(Json.fromInt, e.pp_xoffset_min)),
        ("pp_xoffset_max", handleNull(Json.fromInt, e.pp_xoffset_max)),
        ("pp_yoffset_min", handleNull(Json.fromInt, e.pp_yoffset_min)),
        ("pp_yoffset_max", handleNull(Json.fromInt, e.pp_yoffset_max)),
        ("useragent", handleNull(Json.fromString, e.useragent)),
        ("br_name", handleNull(Json.fromString, e.br_name)),
        ("br_family", handleNull(Json.fromString, e.br_family)),
        ("br_version", handleNull(Json.fromString, e.br_version)),
        ("br_type", handleNull(Json.fromString, e.br_type)),
        ("br_renderengine", handleNull(Json.fromString, e.br_renderengine)),
        ("br_lang", handleNull(Json.fromString, e.br_lang)),
        ("br_features_pdf", handleNull(Json.fromInt, e.br_features_pdf)(_.intValue())),
        ("br_features_flash", handleNull(Json.fromInt, e.br_features_flash)(_.intValue())),
        ("br_features_java", handleNull(Json.fromInt, e.br_features_java)(_.intValue())),
        ("br_features_director", handleNull(Json.fromInt, e.br_features_director)(_.intValue())),
        ("br_features_quicktime", handleNull(Json.fromInt, e.br_features_quicktime)(_.intValue())),
        ("br_features_realplayer", handleNull(Json.fromInt, e.br_features_realplayer)(_.intValue())),
        ("br_features_windowsmedia", handleNull(Json.fromInt, e.br_features_windowsmedia)(_.intValue())),
        ("br_features_gears", handleNull(Json.fromInt, e.br_features_gears)(_.intValue())),
        ("br_features_silverlight", handleNull(Json.fromInt, e.br_features_silverlight)(_.intValue())),
        ("br_cookies", handleNull(Json.fromInt, e.br_cookies)(_.intValue())),
        ("br_colordepth", handleNull(Json.fromString, e.br_colordepth)),
        ("br_viewwidth", handleNull(Json.fromInt, e.br_viewwidth)),
        ("br_viewheight", handleNull(Json.fromInt, e.br_viewheight)),
        ("os_name", handleNull(Json.fromString, e.os_name)),
        ("os_family", handleNull(Json.fromString, e.os_family)),
        ("os_manufacturer", handleNull(Json.fromString, e.os_manufacturer)),
        ("os_timezone", handleNull(Json.fromString, e.os_timezone)),
        ("dvce_type", handleNull(Json.fromString, e.dvce_type)),
        ("dvce_ismobile", handleNull(Json.fromInt, e.dvce_ismobile)(_.intValue())),
        ("dvce_screenwidth", handleNull(Json.fromInt, e.dvce_screenwidth)),
        ("dvce_screenheight", handleNull(Json.fromInt, e.dvce_screenheight)),
        ("doc_charset", handleNull(Json.fromString, e.doc_charset)),
        ("doc_width", handleNull(Json.fromInt, e.doc_width)),
        ("doc_height", handleNull(Json.fromInt, e.doc_height)),
        ("tr_currency", handleNull(Json.fromString, e.tr_currency)),
        ("tr_total_base", handleNull(Json.fromString, e.tr_total_base)),
        ("tr_tax_base", handleNull(Json.fromString, e.tr_tax_base)),
        ("tr_shipping_base", handleNull(Json.fromString, e.tr_shipping_base)),
        ("ti_currency", handleNull(Json.fromString, e.ti_currency)),
        ("ti_price_base", handleNull(Json.fromString, e.ti_price_base)),
        ("base_currency", handleNull(Json.fromString, e.base_currency)),
        ("geo_timezone", handleNull(Json.fromString, e.geo_timezone)),
        ("mkt_clickid", handleNull(Json.fromString, e.mkt_clickid)),
        ("mkt_network", handleNull(Json.fromString, e.mkt_network)),
        ("etl_tags", handleNull(Json.fromString, e.etl_tags)),
        ("dvce_sent_tstamp", handleNull(Json.fromString, e.dvce_sent_tstamp)),
        ("refr_domain_userid", handleNull(Json.fromString, e.refr_domain_userid)),
        ("refr_dvce_tstamp", handleNull(Json.fromString, e.refr_dvce_tstamp)),
        ("derived_contexts", handleNull(Json.fromString, e.derived_contexts)),
        ("domain_sessionid", handleNull(Json.fromString, e.domain_sessionid)),
        ("derived_tstamp", handleNull(Json.fromString, e.derived_tstamp)),
        ("event_vendor", handleNull(Json.fromString, e.event_vendor)),
        ("event_name", handleNull(Json.fromString, e.event_name)),
        ("event_format", handleNull(Json.fromString, e.event_format)),
        ("event_version", handleNull(Json.fromString, e.event_version)),
        ("event_fingerprint", handleNull(Json.fromString, e.event_fingerprint)),
        ("true_tstamp", handleNull(Json.fromString, e.true_tstamp))
      )
    }


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
