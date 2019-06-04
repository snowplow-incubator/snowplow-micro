# Snowplow Micro

Snowplow Micro is a standalone application whose purpose is to automate the testing of trackers.
It receives the tracking events, as a collector does, validates them,
and keeps the results in a cache in memory.
It then offers a REST API to query these events.

The validation of events uses [Iglu](https://github.com/snowplow/iglu)
and requires Iglu resolvers to be set.

An event that was successfully validated will be called a good event, while an event that failed validation will be called a bad event.

## 1. REST API

Snowplow Micro offers 4 endpoints to query the cache.

### 1.1. `/micro/all`: summary

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

### 1.2. `/micro/good` : good events

Query the good events (events that have been successfully validated).

#### HTTP method

- `GET`: get *all* the good events from the cache.
- `POST`: get the good events with the possibility to filter.

#### Response format

JSON array of [GoodEvent](src/main/scala/com.snowplowanalytics.snowplow.micro/model.scala#L19)s. A `GoodEvent` contains 4 fields:
- `event`: contains the [RawEvent](https://github.com/snowplow/snowplow/blob/master/3-enrich/scala-common-enrich/src/main/scala/com.snowplowanalytics.snowplow.enrich/common/adapters/RawEvent.scala#L27). It corresponds to the format of a validated event
just before being enriched.
- `eventType`: type of the event.
- `schema`: schema of the event in case of an unstructured event.
- `contexts`: contexts of the event.

An example of a response with one event can be found below:
```json
[
  {
    "event": {
      "api": {
        "vendor":"com.snowplowanalytics.snowplow",
        "version":"tp2"
      },
      "parameters": {
        "e":"ue",
        "eid":"966d4d79-11d9-4fa6-a1a5-6a0bc2d06de1",
        "aid":"DemoID",
        "cx":"ewoJInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9jb250ZXh0cy9qc29uc2NoZW1hLzEtMC0xIiwKCQkiZGF0YSI6IFsKCQl7CgkJCSJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvY2xpZW50X3Nlc3Npb24vanNvbnNjaGVtYS8xLTAtMSIsCgkJCSJkYXRhIjogewoJCQkJInNlc3Npb25JbmRleCI6IDIsCgkJCQkic3RvcmFnZU1lY2hhbmlzbSI6ICJTUUxJVEUiLAoJCQkJImZpcnN0RXZlbnRJZCI6ICJhZmU0ZTk3Zi00OTNhLTRkNjktOTcxNy05ZGQ3NWVlMjZiMDgiLAoJCQkJInNlc3Npb25JZCI6ICJiNDQzMWExZi04MDEzLTQ0M2UtYWUyMS0yZGI3NDA5ODE0ZDgiLAoJCQkJInByZXZpb3VzU2Vzc2lvbklkIjogImFiYThlYWM1LTQ1M2YtNDZlMy1hNTA3LTZkODAzODNkM2U2NiIsCgkJCQkidXNlcklkIjogIjlhZGRhZWU0LTk0YTktNGE1MS04YjcwLTNjNTM0YTY2OTFiOSIKCQkJfQoJCX0sCgkJewoJCQkic2NoZW1hIjogImlnbHU6Y29tLnNub3dwbG93YW5hbHl0aWNzLnNub3dwbG93L21vYmlsZV9jb250ZXh0L2pzb25zY2hlbWEvMS0wLTEiLAoJCQkiZGF0YSI6IHsKCQkJCSJuZXR3b3JrVGVjaG5vbG9neSI6ICJMVEUiLAoJCQkJImNhcnJpZXIiOiAiVHVyayBUZWxla29tIiwKCQkJCSJvc1ZlcnNpb24iOiAiOC4wLjAiLAoJCQkJIm9zVHlwZSI6ICJhbmRyb2lkIiwKCQkJCSJkZXZpY2VNb2RlbCI6ICJNSSA1IiwKCQkJCSJkZXZpY2VNYW51ZmFjdHVyZXIiOiAiWGlhb21pIiwKCQkJCSJuZXR3b3JrVHlwZSI6ICJtb2JpbGUiCgkJCX0KCQl9LAoJCXsKCQkJInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5tb2JpbGUvc2NyZWVuL2pzb25zY2hlbWEvMS0wLTAiLAoJCQkiZGF0YSI6IHsKCQkJCSJhY3Rpdml0eSI6ICJEZW1vIiwKCQkJCSJuYW1lIjogIkRlbW8iLAoJCQkJImlkIjogIjZkZDMxMTI3LWE2ZmQtNDBkMi04MzkxLTRiOGE2YmM5NzI2YyIsCgkJCQkidHlwZSI6ICJEZW1vIgoJCQl9CgkJfSwKCQl7CgkJCSJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3MubW9iaWxlL2FwcGxpY2F0aW9uL2pzb25zY2hlbWEvMS0wLTAiLAoJCQkiZGF0YSI6IHsKCQkJCSJidWlsZCI6ICIzIiwKCQkJCSJ2ZXJzaW9uIjogIjAuMy4wIgoJCQl9CgkJfQoJXQp9Cg==",
        "tna":"SnowplowAndroidTrackerDemo",
        "tz":"Europe/Istanbul",
        "tv":"andr-1.1.0",
        "res":"1080x1920",
        "p":"mob",
        "dtm":"1433791172",
        "lang":"English",
        "ue_px":"ewogICJzY2hlbWEiOiAiaWdsdTpjb20uc25vd3Bsb3dhbmFseXRpY3Muc25vd3Bsb3cvdW5zdHJ1Y3RfZXZlbnQvanNvbnNjaGVtYS8xLTAtMCIsCiAgICAiZGF0YSI6IHsKICAgICAgInNjaGVtYSI6ICJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvdy9saW5rX2NsaWNrL2pzb25zY2hlbWEvMS0wLTEiLAogICAgICAiZGF0YSI6IHsKICAgICAgICAidGFyZ2V0VXJsIjogImh0dHA6Ly9hLXRhcmdldC11cmwuY29tIgogICAgICB9CiAgICB9Cn0K"
      },
      "contentType":"application/json",
      "source": {
        "name":"ssc-0.15.0-stdout$",
        "encoding":"UTF-8",
        "hostname":"localhost"
      },
      "context": {
        "timestamp":"2019-06-03T12:12:15.416Z",
        "ipAddress":"0:0:0:0:0:0:0:1",
        "useragent":"curl/7.52.1",
        "refererUri":null,
        "headers":[
          "Host: localhost:9090",
          "User-Agent: curl/7.52.1",
          "Accept: */*",
          "Expect: 100-continue",
          "Timeout-Access: <function1>",
          "application/json"
        ],
        "userId":"d04c787c-cbd0-420c-811e-8efb2a0b5e8b"
      }
    },
    "eventType":"ue",
    "schema":"iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1",
    "contexts":[
      "iglu:com.snowplowanalytics.snowplow/client_session/jsonschema/1-0-1",
      "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1",
      "iglu:com.snowplowanalytics.mobile/screen/jsonschema/1-0-0",
      "iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0"
    ]
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

### 1.3. `/micro/bad`: bad events

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

### 1.4. `/micro/reset`: empty cache

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

## 2. Use case

It's very important to keep in mind that Snowplow Micro is designed only to ease the testing of trackers.
As all the events are kept in memory, it should never be used to receive all the events of a pipeline.

## 3. How to run ?

1. Update [configuration for Iglu resolvers](./example/iglu.json)
2. Update [configuration for Snowplow Micro](./example/micro.conf)
3. Run: `sbt "run --collector-config example/micro.conf --iglu example/iglu.json"`