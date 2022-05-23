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

import org.joda.time.DateTime

import java.util.UUID

import org.apache.thrift.TSerializer

import com.snowplowanalytics.snowplow.CollectorPayload.thrift.model1.{CollectorPayload => tCollectorPayload}

import com.snowplowanalytics.snowplow.enrich.common.adapters.RawEvent
import com.snowplowanalytics.snowplow.enrich.common.loaders.CollectorPayload.{Api, Context, Source}

object events {
  val schemaLinkClick = "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1"
  val schemaMobileContext = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1"
  val schemaDoesNotExist = "iglu:com.doesnotexist/does_not_exist/jsonschema/1-0-1"

  val sdjLinkClick = s"""
  {
    "schema": "$schemaLinkClick",
     "data": {
       "targetUrl": "http://a-target-url.com"
     }
  }
  """
  val sdjMobileContext = s"""
  {
    "schema": "$schemaMobileContext",
    "data": {
      "networkTechnology": "LTE",
      "carrier": "Turk Telekom",
      "osVersion": "8.0.0",
      "osType": "android",
      "deviceModel": "MI 5",
      "deviceManufacturer": "Xiaomi",
      "networkType": "mobile"
    }
  }
  """

  val sdjInvalid = s"""
  {
    "schema": "$schemaLinkClick",
     "data": {
       "foo": "bar"
     }
  }
  """

  val sdjDoesNotExist = s"""
  {
    "schema": "$schemaDoesNotExist",
     "data": {
       "foo": "bar"
     }
  }
  """

  def buildRawEvent(unstruct: Option[String] = None, contexts: Option[String] = None): RawEvent = {
    val collectorApi = Api("com.snowplowanalytics.snowplow", "tp2")
    var params = Map(
      "tv" -> "tracker v42",
      "p" -> "web",
      "e" -> "pp"
    )

    params = unstruct match {
      case Some(ue) => params ++ Map ("e" -> "ue", "ue_pr" -> ue)
      case None => params
    }

    params = contexts match {
      case Some(cxs) => params ++ Map ("co" -> cxs)
      case None => params
    }

    RawEvent(
      collectorApi,
      params.mapValues(Some(_)),
      contentType = None,
      Source("Micro", "UTF-8", Some("localhost")),
      Context(
        Some(DateTime.now()),
        Some("0:0:0:0:0:0:0:1"),
        Some("curl/7.52.1"),
        None,
        List(
          "Host: localhost:9090",
          "User-Agent: curl/7.52.1",
          "Accept: */*",
          "Expect: 100-continue",
          "Timeout-Access: <function1>",
          "application/json"
        ),
        Some(UUID.randomUUID())
      )
    )
  }

  def buildUnstruct(sdj: String) = s"""
  {
    "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
    "data": $sdj
  }
  """

  def buildContexts(sdjs: List[String]) = s"""
  {
    "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-1",
    "data": [ ${sdjs.mkString(",")} ]
  }
  """

  def buildThriftBytesBadCollectorPayload(): Array[Byte] = {
    val tCP = new tCollectorPayload(
      "iglu:com.snowplowanalytics.snowplow/CollectorPayload/thrift/1-0-0",
      "1.2.3.4",
      System.currentTimeMillis,
      "UTF-8",
      "Test Micro"
    )
    tCP.setPath("bad/path")

    val ThriftSerializer = new ThreadLocal[TSerializer] {
      override def initialValue = new TSerializer()
    }
    val serializer = ThriftSerializer.get()
    serializer.serialize(tCP)
  }

  def buildThriftBytes1Good1Bad(): Array[Byte] = {
    val tCP = new tCollectorPayload(
      "iglu:com.snowplowanalytics.snowplow/CollectorPayload/thrift/1-0-0",
      "1.2.3.4",
      System.currentTimeMillis,
      "UTF-8",
      "Test Micro"
    )
    tCP.setPath("com.snowplowanalytics.snowplow/tp2")
    val body = s"""
    {
      "schema":"iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
      "data":[
        {"e":"pv","url":"https://console.snowplowanalytics.com/?code=3EaD7VJ2f_aZn33R&state=Y1F6eHNnelUwRVR0c1Q4N3Y3RVM0NlFjdEFLRll6NmsxSnEuZzl%2BTktTQg%3D%3D","page":"Snowplow Insights","refr":"https://id.snowplowanalytics.com/u/login?state=g6Fo2SBDbWRWUmJHTXlHM05pTXhkRTliUUo0YlVIWnVtd0lwUqN0aWTZIHdaQmRyV3NuTTlvNFVZX2tyNUt2MEljcDFGRm9lakFPo2NpZNkgMllzeVFqRHJDTVhoRmRqeXI1MmZ6NDZqUXNlQVpuUUY","tv":"js-2.10.2","tna":"snplow5","aid":"console","p":"web","tz":"America/Chicago","lang":"en-US","cs":"UTF-8","f_pdf":"1","f_qt":"0","f_realp":"0","f_wma":"0","f_dir":"0","f_fla":"0","f_java":"1","f_gears":"0","f_ag":"0","res":"1792x1120","cd":"24","cookie":"1","eid":"1a950884-61d4-4179-a89a-43b67fb58a8f","dtm":"1581382581877","cx":"eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9jb250ZXh0cy9qc29uc2NoZW1hLzEtMC0wIiwiZGF0YSI6W3sic2NoZW1hIjoiaWdsdTpjb20uZ29vZ2xlLmFuYWx5dGljcy9jb29raWVzL2pzb25zY2hlbWEvMS0wLTAiLCJkYXRhIjp7Il9nYSI6IkdBMS4yLjkwOTgzMDA0OS4xNTgxMDgxMzMzIn19LHsic2NoZW1hIjoiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvd2ViX3BhZ2UvanNvbnNjaGVtYS8xLTAtMCIsImRhdGEiOnsiaWQiOiIxNTNmOTA1ZC1iZGFjLTRhYzUtODI2Yy04YWExNmNmMTNmMzkifX0seyJzY2hlbWEiOiJpZ2x1Om9yZy53My9QZXJmb3JtYW5jZVRpbWluZy9qc29uc2NoZW1hLzEtMC0wIiwiZGF0YSI6eyJuYXZpZ2F0aW9uU3RhcnQiOjE1ODEzODI1ODA3MDYsInVubG9hZEV2ZW50U3RhcnQiOjAsInVubG9hZEV2ZW50RW5kIjowLCJyZWRpcmVjdFN0YXJ0IjowLCJyZWRpcmVjdEVuZCI6MCwiZmV0Y2hTdGFydCI6MTU4MTM4MjU4MTM3OCwiZG9tYWluTG9va3VwU3RhcnQiOjE1ODEzODI1ODEzNzgsImRvbWFpbkxvb2t1cEVuZCI6MTU4MTM4MjU4MTM3OCwiY29ubmVjdFN0YXJ0IjoxNTgxMzgyNTgxMzc4LCJjb25uZWN0RW5kIjoxNTgxMzgyNTgxMzc4LCJzZWN1cmVDb25uZWN0aW9uU3RhcnQiOjAsInJlcXVlc3RTdGFydCI6MTU4MTM4MjU4MTM3OCwicmVzcG9uc2VTdGFydCI6MTU4MTM4MjU4MTUwMCwicmVzcG9uc2VFbmQiOjE1ODEzODI1ODE1MTAsImRvbUxvYWRpbmciOjE1ODEzODI1ODE1MTEsImRvbUludGVyYWN0aXZlIjoxNTgxMzgyNTgxNjg5LCJkb21Db250ZW50TG9hZGVkRXZlbnRTdGFydCI6MTU4MTM4MjU4MTY4OSwiZG9tQ29udGVudExvYWRlZEV2ZW50RW5kIjoxNTgxMzgyNTgxNjg5LCJkb21Db21wbGV0ZSI6MCwibG9hZEV2ZW50U3RhcnQiOjAsImxvYWRFdmVudEVuZCI6MH19XX0","vp":"1792x1034","ds":"1792x1034","vid":"4","sid":"82138304-11e6-4999-92a9-2b99118ec50a","duid":"27b9c270-8741-4b77-8082-14c76341d33e","stm":"1581382583373"},
        {"e":"ue","ue_px":"eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy91bnN0cnVjdF9ldmVudC9qc29uc2NoZW1hLzEtMC0wIiwiZGF0YSI6eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9hcHBsaWNhdGlvbl9lcnJvci9qc29uc2NoZW1hLzEtMC0xIiwiZGF0YSI6eyJwcm9ncmFtbWluZ0xhbmd1YWdlIjoiSkFWQVNDUklQVCIsIm1lc3NhZ2UiOiJBUElfRVJST1IgLSA0MDMgLSAiLCJzdGFja1RyYWNlIjpudWxsLCJsaW5lTnVtYmVyIjowLCJsaW5lQ29sdW1uIjowLCJmaWxlTmFtZSI6IiJ9fX0","tv":"js-2.10.2","tna":"snplow5","aid":"console","p":"web","tz":"America/Chicago","lang":"en-US","cs":"UTF-8","f_pdf":"1","f_qt":"0","f_realp":"0","f_wma":"0","f_dir":"0","f_fla":"0","f_java":"1","f_gears":"0","f_ag":"0","res":"1792x1120","cd":"24","cookie":"1","eid":"e94b4262-63e5-4811-b5db-21c4bd2ab354","dtm":"1581382582388","cx":"eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9jb250ZXh0cy9qc29uc2NoZW1hLzEtMC0wIiwiZGF0YSI6W3sic2NoZW1hIjoiaWdsdTpjb20uZ29vZ2xlLmFuYWx5dGljcy9jb29raWVzL2pzb25zY2hlbWEvMS0wLTAiLCJkYXRhIjp7Il9nYSI6IkdBMS4yLjkwOTgzMDA0OS4xNTgxMDgxMzMzIn19LHsic2NoZW1hIjoiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvd2ViX3BhZ2UvanNvbnNjaGVtYS8xLTAtMCIsImRhdGEiOnsiaWQiOiI0MDVlM2U4My1mOGQ3LTRmM2MtYWE5Zi1jNjQ4MzU5NjRhYzkifX0seyJzY2hlbWEiOiJpZ2x1Om9yZy53My9QZXJmb3JtYW5jZVRpbWluZy9qc29uc2NoZW1hLzEtMC0wIiwiZGF0YSI6eyJuYXZpZ2F0aW9uU3RhcnQiOjE1ODEzODI1ODA3MDYsInVubG9hZEV2ZW50U3RhcnQiOjAsInVubG9hZEV2ZW50RW5kIjowLCJyZWRpcmVjdFN0YXJ0IjowLCJyZWRpcmVjdEVuZCI6MCwiZmV0Y2hTdGFydCI6MTU4MTM4MjU4MTM3OCwiZG9tYWluTG9va3VwU3RhcnQiOjE1ODEzODI1ODEzNzgsImRvbWFpbkxvb2t1cEVuZCI6MTU4MTM4MjU4MTM3OCwiY29ubmVjdFN0YXJ0IjoxNTgxMzgyNTgxMzc4LCJjb25uZWN0RW5kIjoxNTgxMzgyNTgxMzc4LCJzZWN1cmVDb25uZWN0aW9uU3RhcnQiOjAsInJlcXVlc3RTdGFydCI6MTU4MTM4MjU4MTM3OCwicmVzcG9uc2VTdGFydCI6MTU4MTM4MjU4MTUwMCwicmVzcG9uc2VFbmQiOjE1ODEzODI1ODE1MTAsImRvbUxvYWRpbmciOjE1ODEzODI1ODE1MTEsImRvbUludGVyYWN0aXZlIjoxNTgxMzgyNTgxNjg5LCJkb21Db250ZW50TG9hZGVkRXZlbnRTdGFydCI6MTU4MTM4MjU4MTY4OSwiZG9tQ29udGVudExvYWRlZEV2ZW50RW5kIjoxNTgxMzgyNTgxNjg5LCJkb21Db21wbGV0ZSI6MTU4MTM4MjU4MTg3OSwibG9hZEV2ZW50U3RhcnQiOjE1ODEzODI1ODE4NzksImxvYWRFdmVudEVuZCI6MTU4MTM4MjU4MTg3OX19LHsic2NoZW1hIjoiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3MuY29uc29sZS91c2VyL2pzb25zY2hlbWEvMS0wLTAiLCJkYXRhIjp7InVzZXJJZCI6IjIyNTIzOTg1LTUxMzMtNDk1YS05ZTY5LWM3YzIzMDVkOWMxMyIsImZpcnN0TmFtZSI6IlRpIiwibGFzdE5hbWUiOiJMaXB0YWsifX0seyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5jb25zb2xlL29yZ2FuaXphdGlvbi9qc29uc2NoZW1hLzEtMC0wIiwiZGF0YSI6eyJuYW1lIjoiQmlnIFRyZWUiLCJvcmdhbml6YXRpb25JZCI6IjZjZTI0Y2YxLTEzMzAtNGRmYi05YTk0LWVmOTEyNzkxYzE2MCJ9fV19","vp":"1792x1034","ds":"1792x1034","vid":"4","sid":"82138304-11e6-4999-92a9-2b99118ec50a","duid":"27b9c270-8741-4b77-8082-14c76341d33e","refr":"https://console.snowplowanalytics.com/?code=3EaD7VJ2f_aZn33R&state=Y1F6eHNnelUwRVR0c1Q4N3Y3RVM0NlFjdEFLRll6NmsxSnEuZzl%2BTktTQg%3D%3D","url":"https://console.snowplowanalytics.com/","stm":"1581382583373"}
      ]
    }
    """
    tCP.setBody(body)
    tCP.setContentType("application/json")

    val ThriftSerializer = new ThreadLocal[TSerializer] {
      override def initialValue = new TSerializer()
    }
    val serializer = ThriftSerializer.get()
    serializer.serialize(tCP)
  }
}
