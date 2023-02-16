# Snowplow Micro

[![Docker Image Version (latest semver)][docker-image]][docker-micro]
[![Docker pulls][docker-pulls]][docker-micro]
[![Build Status][gh-actions-image]][gh-actions]
[![License][license-image]][license]

Snowplow Micro is a lightweight version of the Snowplow pipeline. It’s great for:
* Getting familiar with Snowplow
* Debugging and testing, including automated testing

Just like a real Snowplow pipeline, Micro receives and validates events sent by your tracking code.

Learn more in the documentation: https://docs.snowplow.io/docs/getting-started-with-micro/basic-usage/

---

## Maintainer quick start

Assuming [Git][git] and [sbt][sbt]:

```text
git clone git@github.com:snowplow-incubator/snowplow-micro.git
cd snowplow-micro

git clone --branch 2.8.1 --depth 1 git@github.com:snowplow/stream-collector.git
cd stream-collector
sbt publishLocal && cd ..

sbt test
```

## Copyright and License

Snowplow Micro is copyright 2019-2023 Snowplow Analytics Ltd.

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
[distroless-repo]: https://github.com/GoogleContainerTools/distroless

[gh-actions]: https://github.com/snowplow-incubator/snowplow-micro/actions
[gh-actions-image]: https://github.com/snowplow-incubator/snowplow-micro/actions/workflows/test.yml/badge.svg?branch=master
[gh-releases]: https://github.com/snowplow-incubator/snowplow-micro/releases

[license]: https://www.apache.org/licenses/LICENSE-2.0
[license-image]: https://img.shields.io/badge/license-Apache--2-blue.svg?style=flat

[snowplow]: https://github.com/snowplow/snowplow
[discourse]: https://discourse.snowplow.io

[example-dir]: https://github.com/snowplow-incubator/snowplow-micro/tree/master/example

[iglu]: https://github.com/snowplow/iglu
[iglu-resolver-config]: https://docs.snowplow.io/docs/pipeline-components-and-applications/iglu/iglu-resolver/
[iglu-resolver-example]: https://github.com/snowplow-incubator/snowplow-micro/blob/master/example/iglu.json

[stream-collector]: https://github.com/snowplow/stream-collector
[stream-collector-config]: https://docs.snowplow.io/docs/pipeline-components-and-applications/stream-collector/configure/#basic-configuration
[collector-config-example]: https://github.com/snowplow-incubator/snowplow-micro/blob/master/example/micro.conf

[snowplow-trackers]: https://docs.snowplow.io/docs/collecting-data/collecting-from-own-applications/
[micro-rest-api]: https://docs.snowplow.io/docs/managing-data-quality/testing-and-qa-workflows/set-up-automated-testing-with-snowplow-micro/#rest-api

[techdocs]: https://docs.snowplow.io/docs/managing-data-quality/testing-and-qa-workflows/set-up-automated-testing-with-snowplow-micro
[techdocs-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/techdocs.png
[roadmap]: https://github.com/snowplow/snowplow/projects/7
[roadmap-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/roadmap.png
[contributing]: https://docs.snowplow.io/docs/contributing
[contributing-image]: https://d3i6fms1cm1j0i.cloudfront.net/github/images/contributing.png

[git]: https://git-scm.com/
[sbt]: https://www.scala-sbt.org/
