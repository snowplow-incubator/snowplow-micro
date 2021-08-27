# Snowplow Micro

[![Docker Image Version (latest semver)][docker-image]][docker-micro]
[![Docker pulls][docker-pulls]][docker-micro]
[![Build Status][gh-actions-image]][gh-actions]
[![License][license-image]][license]

Snowplow Micro is built to enable companies running [Snowplow][snowplow] to build automated CI/CD workflows ensuring upstream data quality.

It is a very small version of a full Snowplow data collection pipeline: small enough that it can be launched by an automated test suite. Snowplow Micro then exposes an API, that can be queried in order to validate your data collection setup as your digital products evolve.

## Quick start

Snowplow Micro is configured at runtime through the [Stream Collector][stream-collector] configuration and the [Iglu][iglu] resolver configuration. So, to start Snowplow Micro:

1. Update the [stream collector configuration][stream-collector-config] for Snowplow Micro: [example configuration][collector-config-example].
2. Update the [Iglu resolver configuration][iglu-resolver-config] for Snowplow Micro: [example configuration][iglu-resolver-example].
3. Run Micro either using Docker or using Java.

### Using Docker

Snowplow Micro is hosted on Docker Hub ([snowplow/snowplow-micro][docker-micro]) with images available for both `amd64` and `arm64` architectures.

The configuration files must be placed in a folder that is mounted in the Docker container, and the port configured for Micro needs to be exposed. The following examples assume that the configuration files are in the directory `./example/` and that port `9090` is the one to bind on your host machine:

```bash
docker run \
  --mount type=bind,source=$(pwd)/example,destination=/config \
  -p 9090:9090 \
  snowplow/snowplow-micro:1.2.1 \
  --collector-config /config/micro.conf \
  --iglu /config/iglu.json
```

In order to use the embedded Iglu capabilities, the command is the same as above. You only need to add your local Iglu repository in the configurations directory named as `iglu-client-embedded`, as also shown in the [example][example-dir]:

```text
example
├── iglu-client-embedded
│   └── schemas
│       └── com.myvendor
│           └── myschema
│               └── jsonschema
│                   └── 1-0-0
├── iglu.json
└── micro.conf
```

### Using Java

If you cannot use Docker, a Snowplow Micro jar file is hosted on the [Github releases][gh-releases]. As an example, run Micro as:

```bash
java -jar snowplow-micro-1.2.1-rc1.jar --collector-config example/micro.conf --iglu example/iglu.json
```

In case you also want an embedded Iglu, apply the same directory structure as shown above and run for example:

```bash
# Unix
java -cp snowplow-micro-1.2.1-rc1.jar:example com.snowplowanalytics.snowplow.micro.Main --collector-config example/micro.conf --iglu example/iglu.json
# Windows
java -cp snowplow-micro-1.2.1-rc1.jar;example com.snowplowanalytics.snowplow.micro.Main --collector-config example/micro.conf --iglu example/iglu.json
```

### Send events and start testing

Once you have successfully started Snowplow Micro, its collector endpoint will be `http://localhost:{PORT}`, e.g. `http://localhost:9090`, to which you can point any of the [Snowplow Trackers][snowplow-trackers] and start sending events.

Then, in your testing workflows, you can use Micro's [REST API][micro-rest-api] endpoints to assert on the collected events:

- `/micro/all`: summary
- `/micro/good`: good events
- `/micro/bad`: bad events
- `/micro/reset`: clears cache

Additionally, if you are using the embedded Iglu capabilities, you can also use the `/micro/iglu` endpoint to check whether your custom schemas can be resolved as:

- `/micro/iglu/{vendorName}/{schemaName}/jsonschema/{schemaVersion}` (for example `/micro/iglu/com.myvendor/myschema/jsonschema/1-0-0`)

## Find out more

| Technical Docs                    | Roadmap                         | Contributing                              |
|:---------------------------------:|:-------------------------------:|:-----------------------------------------:|
| [![i1][techdocs-image]][techdocs] | [![i2][roadmap-image]][roadmap] | [![i3][contributing-image]][contributing] |
| [Technical Docs][techdocs]        | [Roadmap][roadmap]              | [Contributing][contributing]              |

## Maintainer quick start

Assuming [Git][git] and [sbt][sbt]:

```text
git clone git@github.com:snowplow-incubator/snowplow-micro-examples.git
cd snowplow-micro

git clone --branch 2.3.1 --depth 1 git@github.com:snowplow/stream-collector.git
cd stream-collector
sbt publishLocal && cd ..

sbt test
```

## Copyright and License

Snowplow Micro is copyright 2019-2021 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[docker-micro]: https://hub.docker.com/r/snowplow/snowplow-micro
[docker-image]: https://img.shields.io/docker/v/snowplow/snowplow-micro?sort=semver
[docker-pulls]: https://img.shields.io/docker/pulls/snowplow/snowplow-micro

[gh-actions]: https://github.com/snowplow-incubator/snowplow-micro/actions
[gh-actions-image]: https://github.com/snowplow-incubator/snowplow-micro/actions/workflows/test.yml/badge.svg?branch=master
[gh-releases]: https://github.com/snowplow-incubator/snowplow-micro/releases

[license]: https://www.apache.org/licenses/LICENSE-2.0
[license-image]: https://img.shields.io/badge/license-Apache--2-blue.svg?style=flat

[snowplow]: https://github.com/snowplow/snowplow
[discourse]: https://discourse.snowplowanalytics.com

[example-dir]: https://github.com/snowplow-incubator/snowplow-micro/tree/master/example

[iglu]: https://github.com/snowplow/iglu
[iglu-resolver-config]: https://docs.snowplowanalytics.com/docs/pipeline-components-and-applications/iglu/iglu-resolver/
[iglu-resolver-example]: https://github.com/snowplow-incubator/snowplow-micro/blob/master/example/iglu.json

[stream-collector]: https://github.com/snowplow/stream-collector
[stream-collector-config]: https://docs.snowplowanalytics.com/docs/pipeline-components-and-applications/stream-collector/configure/#basic-configuration
[collector-config-example]: https://github.com/snowplow-incubator/snowplow-micro/blob/master/example/micro.conf

[snowplow-trackers]: https://docs.snowplowanalytics.com/docs/collecting-data/collecting-from-own-applications/
[micro-rest-api]: https://docs.snowplowanalytics.com/docs/managing-data-quality/testing-and-qa-workflows/set-up-automated-testing-with-snowplow-micro/#rest-api

[techdocs]: https://docs.snowplowanalytics.com/docs/managing-data-quality/testing-and-qa-workflows/set-up-automated-testing-with-snowplow-micro
[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[roadmap]: https://github.com/snowplow/snowplow/projects/7
[roadmap-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/roadmap.png
[contributing]: https://docs.snowplowanalytics.com/docs/contributing
[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png

[git]: https://git-scm.com/
[sbt]: https://www.scala-sbt.org/
