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

import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification

class ConfigHelperSpec extends Specification with CatsEffect {
  "ConfigHelper" >> {
    "will produce a valid parsed collector config if `--collector-config` is not present" >> {
     Configuration.loadCollectorConfig(None).value.map {
       case Right(_) => ok
       case Left(_) => ko
     }
    }
  }
}
