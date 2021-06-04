# Snowplow Micro

![Docker Image Version (latest semver)](https://img.shields.io/docker/v/snowplow/snowplow-micro)
![GitHub](https://img.shields.io/github/license/snowplow-incubator/snowplow-micro)

## 1. What is Snowplow Micro for?

Snowplow Micro is built to enable companies running Snowplow to build automated test suites to ensure that new releases of their websites, mobile apps and server-side applications do not break tracking / Snowplow data collection.

Snowplow Micro is a very small version of a full Snowplow data collection pipeline: small enough that it can be launched by a test suite. Events can be recorded into Snowplow Micro just as they can a full Snowplow pipeline. Micro then exposes an API that can be queried to understand:

* How many events have been received?
* How many of them were successfully processed vs ended up as "bad" (e.g. because the events failed validation against the corresponding schemas in the [Iglu](https://github.com/snowplow/iglu) Schema Registry)
* For any events that have successfully processed, what type of events they are, what fields have been recorded etc.
* For any events that have not been successfully processed, what errors were generated on processing the events. (So these can be surfaced back via the test suite.)

This means companies can build automated test suites to ensure that specific events in an application generate specific events that are successfully processed by Snowplow.

## 2. How do I run Snowplow Micro??

#### Using Docker

Snowplow Micro is hosted on Docker Hub : [snowplow/snowplow-micro](https://cloud.docker.com/u/snowplow/repository/docker/snowplow/snowplow-micro/general).

1. Update [configuration for Snowplow Micro](./example/micro.conf)
1. Update [configuration for Iglu resolvers](./example/iglu.json)
1. The configuration files must be placed in a folder that is mounted in the Docker container, and the port configured for Micro needs to be exposed. Example with configuration files in `./example/` and port `9090`:
```
$ docker run --mount type=bind,source=$(pwd)/example,destination=/config -p 9090:9090 snowplow/snowplow-micro:1.1.2 --collector-config /config/micro.conf --iglu /config/iglu.json
```

#### Using Java

Alternatively, a Snowplow Micro jar file is hosted on the [Github release page](https://github.com/snowplow-incubator/snowplow-micro/releases/tag/micro-1.1.2)
```
java -jar snowplow-micro-1.1.2.jar --collector-config example/micro.conf --iglu example/iglu.json
```

#### Use custom schemas from local folder

If `schemas/` folder holding the Iglu schemas is in `$(pwd)/iglu-schemas/`, this parameter must be added to `docker run`:
`--mount type=bind,source=$(pwd)/iglu-schemas,destination=/iglu-schemas`.

The container embeds an HTTP server that serves `/iglu-schemas` and listens to `8080`.
These lines must be added to the resolver:
```
{
  "name": "Micro's Iglu /iglu-schemas",
  "priority": 100,
  "vendorPrefixes": [ "com.snowplowanalytics" ],
  "connection": {
    "http": {
      "uri": "http://localhost:8080"
    }
  }
}
```

It's possible to override the default `8080` by adding `-e IGLU_PORT=xxx` to `docker run` command.

## 3. REST API

Snowplow Micro offers 4 endpoints to query the data recorded.

### 3.1. `/micro/all`: summary

Get a summary with the number of good and bad events currently in the cache.

#### HTTP method

`GET`, `POST`

#### Response format

Example:
```json
{
  "total": 7,
  "good": 5,
  "bad": 2
}
```

### 3.2. `/micro/good` : good events

Query the good events (events that have been successfully validated).

#### HTTP method

- `GET`: get *all* the good events from the cache.
- `POST`: get the good events with the possibility to filter.

#### Response format

JSON array of [GoodEvent](src/main/scala/com.snowplowanalytics.snowplow.micro/model.scala#L19)s. A `GoodEvent` contains 4 fields:
- `rawEvent`: contains the [RawEvent](https://github.com/snowplow/snowplow/blob/master/3-enrich/scala-common-enrich/src/main/scala/com.snowplowanalytics.snowplow.enrich/common/adapters/RawEvent.scala#L27). It corresponds to the format of a validated event
just before being enriched.
- `event`: contains the [canonical snowplow Event](https://github.com/snowplow/snowplow-scala-analytics-sdk/blob/2.0.1/src/main/scala/com.snowplowanalytics.snowplow.analytics.scalasdk/Event.scala#L39).
It is in the format of an event after enrichment, even if all the enrichments are deactivated.
- `eventType`: type of the event.
- `schema`: schema of the event in case of an unstructured event.
- `contexts`: contexts of the event.

An example of a response with one event can be found below:
```json
[
  {
    "rawEvent": {
      "api": {
        "vendor": "com.snowplowanalytics.snowplow",
        "version": "tp2"
      },
      "parameters": {
        "e": "ue",
        "eid": "966d4d79-11d9-4fa6-a1a5-6a0bc2d06de1",
        "aid": "DemoID",
        "cx": "ewoJInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9jb250ZXh0cy9qc29uc2NoZW1hLzEtMC0xIiwKCQkiZGF0YSI6IFsKCQl7CgkJCSJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvY2xpZW50X3Nlc3Npb24vanNvbnNjaGVtYS8xLTAtMSIsCgkJCSJkYXRhIjogewoJCQkJInNlc3Npb25JbmRleCI6IDIsCgkJCQkic3RvcmFnZU1lY2hhbmlzbSI6ICJTUUxJVEUiLAoJCQkJImZpcnN0RXZlbnRJZCI6ICJhZmU0ZTk3Zi00OTNhLTRkNjktOTcxNy05ZGQ3NWVlMjZiMDgiLAoJCQkJInNlc3Npb25JZCI6ICJiNDQzMWExZi04MDEzLTQ0M2UtYWUyMS0yZGI3NDA5ODE0ZDgiLAoJCQkJInByZXZpb3VzU2Vzc2lvbklkIjogImFiYThlYWM1LTQ1M2YtNDZlMy1hNTA3LTZkODAzODNkM2U2NiIsCgkJCQkidXNlcklkIjogIjlhZGRhZWU0LTk0YTktNGE1MS04YjcwLTNjNTM0YTY2OTFiOSIKCQkJfQoJCX0sCgkJewoJCQkic2NoZW1hIjogImlnbHU6Y29tLnNub3dwbG93YW5hbHl0aWNzLnNub3dwbG93L21vYmlsZV9jb250ZXh0L2pzb25zY2hlbWEvMS0wLTEiLAoJCQkiZGF0YSI6IHsKCQkJCSJuZXR3b3JrVGVjaG5vbG9neSI6ICJMVEUiLAoJCQkJImNhcnJpZXIiOiAiVHVyayBUZWxla29tIiwKCQkJCSJvc1ZlcnNpb24iOiAiOC4wLjAiLAoJCQkJIm9zVHlwZSI6ICJhbmRyb2lkIiwKCQkJCSJkZXZpY2VNb2RlbCI6ICJNSSA1IiwKCQkJCSJkZXZpY2VNYW51ZmFjdHVyZXIiOiAiWGlhb21pIiwKCQkJCSJuZXR3b3JrVHlwZSI6ICJtb2JpbGUiCgkJCX0KCQl9LAoJCXsKCQkJInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5tb2JpbGUvc2NyZWVuL2pzb25zY2hlbWEvMS0wLTAiLAoJCQkiZGF0YSI6IHsKCQkJCSJhY3Rpdml0eSI6ICJEZW1vIiwKCQkJCSJuYW1lIjogIkRlbW8iLAoJCQkJImlkIjogIjZkZDMxMTI3LWE2ZmQtNDBkMi04MzkxLTRiOGE2YmM5NzI2YyIsCgkJCQkidHlwZSI6ICJEZW1vIgoJCQl9CgkJfSwKCQl7CgkJCSJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3MubW9iaWxlL2FwcGxpY2F0aW9uL2pzb25zY2hlbWEvMS0wLTAiLAoJCQkiZGF0YSI6IHsKCQkJCSJidWlsZCI6ICIzIiwKCQkJCSJ2ZXJzaW9uIjogIjAuMy4wIgoJCQl9CgkJfQoJXQp9Cg==",
        "tna": "SnowplowAndroidTrackerDemo",
        "tz": "Europe/Istanbul",
        "tv": "andr-1.1.0",
        "res": "1080x1920",
        "p": "mob",
        "dtm": "1433791172",
        "lang": "English",
        "ue_px": "ewogICJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvdW5zdHJ1Y3RfZXZlbnQvanNvbnNjaGVtYS8xLTAtMCIsCiAgICAiZGF0YSI6IHsKICAgICAgInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9saW5rX2NsaWNrL2pzb25zY2hlbWEvMS0wLTEiLAogICAgICAiZGF0YSI6IHsKICAgICAgICAidGFyZ2V0VXJsIjogImh0dHA6Ly9hLXRhcmdldC11cmwuY29tIgogICAgICB9CiAgICB9Cn0K"
      },
      "contentType": "application/json",
      "source": {
        "name": "ssc-1.0.1-stdout$",
        "encoding": "UTF-8",
        "hostname": "localhost"
      },
      "context": {
        "timestamp": "2020-09-04T14:23:56.702Z",
        "ipAddress": "127.0.0.1",
        "useragent": "curl/7.68.0",
        "refererUri": null,
        "headers": [
          "Timeout-Access: <function1>",
          "Host: localhost:9090",
          "User-Agent: curl/7.68.0",
          "Accept: */*",
          "Expect: 100-continue",
          "application/json"
        ],
        "userId": "7189e4ca-e11f-4c7b-aec0-e0401f049416"
      }
    },
    "eventType": "unstruct",
    "schema": "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
    "contexts": [
      "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-1",
      "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1",
      "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-0-0",
      "iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0"
    ],
    "event": {
      "app_id": "DemoID",
      "platform": "mob",
      "etl_tstamp": "2020-09-04T14:23:57.344Z",
      "collector_tstamp": "2020-09-04T14:23:56.702Z",
      "dvce_created_tstamp": "1970-01-17T14:16:31.172Z",
      "event": "unstruct",
      "event_id": "966d4d79-11d9-4fa6-a1a5-6a0bc2d06de1",
      "txn_id": null,
      "name_tracker": "SnowplowAndroidTrackerDemo",
      "v_tracker": "andr-1.1.0",
      "v_collector": "ssc-1.0.1-stdout$",
      "v_etl": "snowplow-micro-0.1.0-common-1.3.2",
      "user_id": null,
      "user_ipaddress": "127.0.0.1",
      "user_fingerprint": null,
      "domain_userid": null,
      "domain_sessionidx": null,
      "network_userid": "7189e4ca-e11f-4c7b-aec0-e0401f049416",
      "geo_country": null,
      "geo_region": null,
      "geo_city": null,
      "geo_zipcode": null,
      "geo_latitude": null,
      "geo_longitude": null,
      "geo_region_name": null,
      "ip_isp": null,
      "ip_organization": null,
      "ip_domain": null,
      "ip_netspeed": null,
      "page_url": null,
      "page_title": null,
      "page_referrer": null,
      "page_urlscheme": null,
      "page_urlhost": null,
      "page_urlport": null,
      "page_urlpath": null,
      "page_urlquery": null,
      "page_urlfragment": null,
      "refr_urlscheme": null,
      "refr_urlhost": null,
      "refr_urlport": null,
      "refr_urlpath": null,
      "refr_urlquery": null,
      "refr_urlfragment": null,
      "refr_medium": null,
      "refr_source": null,
      "refr_term": null,
      "mkt_medium": null,
      "mkt_source": null,
      "mkt_term": null,
      "mkt_content": null,
      "mkt_campaign": null,
      "contexts": {
        "schema": "iglu:com.snowplowanalytics.snowplow/contexts/jsonschema/1-0-0",
        "data": [
          {
            "schema": "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-1",
            "data": {
              "sessionIndex": 2,
              "storageMechanism": "SQLITE",
              "firstEventId": "afe4e97f-493a-4d69-9717-9dd75ee26b08",
              "sessionId": "b4431a1f-8013-443e-ae21-2db7409814d8",
              "previousSessionId": "aba8eac5-453f-46e3-a507-6d80383d3e66",
              "userId": "9addaee4-94a9-4a51-8b70-3c534a6691b9"
            }
          },
          {
            "schema": "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1",
            "data": {
              "networkTechnology": "LTE",
              "carrier": "Turk Telekom",
              "osVersion": "8.0.0",
              "osType": "android",
              "deviceModel": "MI 5",
              "deviceManufacturer": "Xiaomi",
              "networkType": "mobile"
            }
          },
          {
            "schema": "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-0-0",
            "data": {
              "activity": "Demo",
              "name": "Demo",
              "id": "6dd31127-a6fd-40d2-8391-4b8a6bc9726c",
              "type": "Demo"
            }
          },
          {
            "schema": "iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0",
            "data": {
              "build": "3",
              "version": "0.3.0"
            }
          }
        ]
      },
      "se_category": null,
      "se_action": null,
      "se_label": null,
      "se_property": null,
      "se_value": null,
      "unstruct_event": {
        "schema": "iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
        "data": {
          "schema": "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
          "data": {
            "targetUrl": "http://a-target-url.com"
          }
        }
      },
      "tr_orderid": null,
      "tr_affiliation": null,
      "tr_total": null,
      "tr_tax": null,
      "tr_shipping": null,
      "tr_city": null,
      "tr_state": null,
      "tr_country": null,
      "ti_orderid": null,
      "ti_sku": null,
      "ti_name": null,
      "ti_category": null,
      "ti_price": null,
      "ti_quantity": null,
      "pp_xoffset_min": null,
      "pp_xoffset_max": null,
      "pp_yoffset_min": null,
      "pp_yoffset_max": null,
      "useragent": "curl/7.68.0",
      "br_name": null,
      "br_family": null,
      "br_version": null,
      "br_type": null,
      "br_renderengine": null,
      "br_lang": "English",
      "br_features_pdf": null,
      "br_features_flash": null,
      "br_features_java": null,
      "br_features_director": null,
      "br_features_quicktime": null,
      "br_features_realplayer": null,
      "br_features_windowsmedia": null,
      "br_features_gears": null,
      "br_features_silverlight": null,
      "br_cookies": null,
      "br_colordepth": null,
      "br_viewwidth": null,
      "br_viewheight": null,
      "os_name": null,
      "os_family": null,
      "os_manufacturer": null,
      "os_timezone": "Europe/Istanbul",
      "dvce_type": null,
      "dvce_ismobile": null,
      "dvce_screenwidth": 1080,
      "dvce_screenheight": 1920,
      "doc_charset": null,
      "doc_width": null,
      "doc_height": null,
      "tr_currency": null,
      "tr_total_base": null,
      "tr_tax_base": null,
      "tr_shipping_base": null,
      "ti_currency": null,
      "ti_price_base": null,
      "base_currency": null,
      "geo_timezone": null,
      "mkt_clickid": null,
      "mkt_network": null,
      "etl_tags": null,
      "dvce_sent_tstamp": null,
      "refr_domain_userid": null,
      "refr_dvce_tstamp": null,
      "derived_contexts": {},
      "domain_sessionid": null,
      "derived_tstamp": "2020-09-04T14:23:56.702Z",
      "event_vendor": "com.snowplowanalytics.snowplow",
      "event_name": "link_click",
      "event_format": "jsonschema",
      "event_version": "1-0-1",
      "event_fingerprint": null,
      "true_tstamp": null
    }
  }
]
```

#### Filters

When querying `/micro/good` with `POST` (`Content-Type: application/json` needs to be set in the headers of the request), it's possible to specify filters,
thanks to a JSON in the data of the HTTP request.

Example of command to query the good events:
`curl -X POST -H 'Content-Type: application/json' <IP:PORT>/micro/good -d '<JSON>'`

An example of JSON with filters could be:
```json
{ 
  "schema": "iglu:com.acme/example/jsonschema/1-0-0",
  "contexts": [
    "com.snowplowanalytics.mobile/application/jsonschema/1-0-0",
    "com.snowplowanalytics.mobile/screen/jsonschema/1-0-0"
  ],
  "limit": 10
}
```

List of possible fields for the filters:
- `event_type`: type of the event (in `e` param);
- `schema`: corresponds to the shema of an [unstructured event](https://github.com/snowplow/snowplow/wiki/snowplow-tracker-protocol#310-custom-unstructured-event-tracking) (schema of the self-describing JSON contained in `ue_pr` or `ue_px`).
It automatically implies `event_type` = `ue`.
- `contexts`: list of the schemas contained in the contexts of an event (parameters `co` or `cx`). An event must contain **all** the contexts of the list to be returned.
It can also contain more contexts than the ones specified in the request.
- `limit`: limit the number of events in the response (most recent events are returned).  

It's not necessary to specify all the fields in a request, only the ones that need to be used for filtering.

### 3.3. `/micro/bad`: bad events

Query the bad events (events that failed validation).

#### HTTP method

- `GET`: get *all* the bad events from the cache.
- `POST`: get the bad events with the possibility to filter.

#### Response format

JSON array of [BadEvent](src/main/scala/com.snowplowanalytics.snowplow.micro/model.scala#L27)s. A `BadEvent` contains 4 fields:
- `collectorPayload`: contains the [CollectorPayload](https://github.com/snowplow/snowplow/blob/master/3-enrich/scala-common-enrich/src/main/scala/com.snowplowanalytics.snowplow.enrich/common/loaders/collectorPayload.scala#L140)
with all the raw information of the tracking event.
This field can be empty if an error occured before trying to validate a payload.
- `errors`: list of errors that occured during the validation of the tracking event.

An example of a response with one event can be found below:
```json
[
  {
    "collectorPayload": {
      "api": {
        "vendor":"com.snowplowanalytics.snowplow",
        "version":"tp2"
      },
      "querystring":[],
      "contentType":"application/json",
      "body":"{\n    \"schema\":\"iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4\",\n    \"data\" : [\n        {\n            \"eid\": \"966d4d79-11d9-4fa6-a1a5-6a0bc2d06de1\",\n            \"res\": \"1080x1920\",\n            \"tv\": \"andr-1.1.0\",\n            \"tna\": \"SnowplowAndroidTrackerDemo\",\n            \"tz\": \"Europe/Istanbul\",\n            \"p\": \"mob\",\n            \"ue_pr\": \"bad custom event\",\n            \"cx\": \"ewoJInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9jb250ZXh0cy9qc29uc2NoZW1hLzEtMC0xIiwKCQkiZGF0YSI6IFsKCQl7CgkJCSJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvY2xpZW50X3Nlc3Npb24vanNvbnNjaGVtYS8xLTAtMSIsCgkJCSJkYXRhIjogewoJCQkJInNlc3Npb25JbmRleCI6IDIsCgkJCQkic3RvcmFnZU1lY2hhbmlzbSI6ICJTUUxJVEUiLAoJCQkJImZpcnN0RXZlbnRJZCI6ICJhZmU0ZTk3Zi00OTNhLTRkNjktOTcxNy05ZGQ3NWVlMjZiMDgiLAoJCQkJInNlc3Npb25JZCI6ICJiNDQzMWExZi04MDEzLTQ0M2UtYWUyMS0yZGI3NDA5ODE0ZDgiLAoJCQkJInByZXZpb3VzU2Vzc2lvbklkIjogImFiYThlYWM1LTQ1M2YtNDZlMy1hNTA3LTZkODAzODNkM2U2NiIsCgkJCQkidXNlcklkIjogIjlhZGRhZWU0LTk0YTktNGE1MS04YjcwLTNjNTM0YTY2OTFiOSIKCQkJfQoJCX0sCgkJewoJCQkic2NoZW1hIjogImlnbHU6Y29tLnNub3dwbG93YW5hbHl0aWNzLnNub3dwbG93L21vYmlsZV9jb250ZXh0L2pzb25zY2hlbWEvMS0wLTEiLAoJCQkiZGF0YSI6IHsKCQkJCSJuZXR3b3JrVGVjaG5vbG9neSI6ICJMVEUiLAoJCQkJImNhcnJpZXIiOiAiVHVyayBUZWxla29tIiwKCQkJCSJvc1ZlcnNpb24iOiAiOC4wLjAiLAoJCQkJIm9zVHlwZSI6ICJhbmRyb2lkIiwKCQkJCSJkZXZpY2VNb2RlbCI6ICJNSSA1IiwKCQkJCSJkZXZpY2VNYW51ZmFjdHVyZXIiOiAiWGlhb21pIiwKCQkJCSJuZXR3b3JrVHlwZSI6ICJtb2JpbGUiCgkJCX0KCQl9LAoJCXsKCQkJInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5tb2JpbGUvc2NyZWVuL2pzb25zY2hlbWEvMS0wLTAiLAoJCQkiZGF0YSI6IHsKCQkJCSJhY3Rpdml0eSI6ICJEZW1vIiwKCQkJCSJuYW1lIjogIkRlbW8iLAoJCQkJImlkIjogIjZkZDMxMTI3LWE2ZmQtNDBkMi04MzkxLTRiOGE2YmM5NzI2YyIsCgkJCQkidHlwZSI6ICJEZW1vIgoJCQl9CgkJfSwKCQl7CgkJCSJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3MubW9iaWxlL2FwcGxpY2F0aW9uL2pzb25zY2hlbWEvMS0wLTAiLAoJCQkiZGF0YSI6IHsKCQkJCSJidWlsZCI6ICIzIiwKCQkJCSJ2ZXJzaW9uIjogIjAuMy4wIgoJCQl9CgkJfQoJXQp9Cg==\",\n            \"dtm\": \"1433791172\",\n            \"lang\": \"English\",\n            \"aid\": \"DemoID\"\n          }\n    ]\n            \n}",
      "source": {
        "name":"ssc-0.15.0-stdout$",
        "encoding":"UTF-8",
        "hostname":"localhost"
      },
      "context": {
        "timestamp":"2019-06-03T13:03:35.786Z",
        "ipAddress":"0:0:0:0:0:0:0:1",
        "useragent":"curl/7.52.1",
        "refererUri":null,
        "headers": [
          "Host: localhost:9090",
          "User-Agent: curl/7.52.1",
          "Accept: */*",
          "Expect: 100-continue",
          "Timeout-Access: <function1>",
          "application/json"
        ],
        "userId":"94004edb-a616-409b-81d7-9762251c2bc6"
      }
    },
    "errors": [
      "Error while extracting event(s) from collector payload and validating it/them.","error: object has missing required properties ([\"e\"])\n    level: \"error\"\n    schema: {\"loadingURI\":\"#\",\"pointer\":\"/items\"}\n    instance: {\"pointer\":\"/0\"}\n    domain: \"validation\"\n    keyword: \"required\"\n    required: [\"e\",\"p\",\"tv\"]\n    missing: [\"e\"]\n"
    ]
  }
]
```

#### Filters

When querying `/micro/bad` with `POST` (`Content-Type: application/json` needs to be set in the headers of the request), it's possible to specify filters,
thanks to a JSON in the data of the HTTP request.

Example of command to query the bad events:
`curl -X POST -H 'Content-Type: application/json' <IP:PORT>/micro/bad -d '<JSON>'`

An example of JSON with filters could be:
```json
{
    "vendor":"com.snowplowanalytics.snowplow",
    "version":"tp2",
    "limit": 10
}
```

List of possible fields for the filters:
- `vendor`: vendor for the tracking event.
- `version`: version of the vendor for the tracking event.
- `limit`: limit the number of events in the response (most recent events are returned).  

It's not necessary to specify all the fields in each request, only the ones that need to be used for filtering.

### 3.4. `/micro/reset`: empty cache

Delete all events from the cache.

#### HTTP method

`GET`, `POST`

#### Response format

Expected:
```json
{
  "total": 0,
  "good": 0,
  "bad": 0
}
```
