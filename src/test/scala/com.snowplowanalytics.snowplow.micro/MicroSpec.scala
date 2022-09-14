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

import org.specs2.mutable.Specification

class MicroSpec extends Specification {
  "Micro" >> {
    // This test is to ensure that if the default collector config, located at
    // `src/main/resources/application.confg` is invalid, the tests will not pass
    "will throw an exception if the default collector config is invalid" >> {
      ConfigHelper.parseConfig(Array(
        "--collector-config",
        getClass.getResource("/application.conf").getPath
      )) must not(throwA[Exception])
    }
  }
}
