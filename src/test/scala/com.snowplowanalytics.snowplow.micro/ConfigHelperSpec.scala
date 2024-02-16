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

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import com.monovore.decline.Command
import com.snowplowanalytics.snowplow.micro.Configuration.MicroConfig
import org.specs2.mutable.Specification

class ConfigHelperSpec extends Specification with CatsEffect {
  "Configuration loader should work when" >> {
    "no custom args are provided and only defaults are used" >> {
      load(args = List.empty)
        .map { result =>
          result must beRight[MicroConfig].like {
            case config =>
              config.collector.port must beEqualTo(9090)
              config.collector.ssl.enable must beFalse 
              config.collector.ssl.port must beEqualTo(9543) 
              
              config.enrichmentsConfig.isEmpty must beTrue
              config.iglu.resolver.repos.map(_.config.name) must containTheSameElementsAs(List("Iglu Central", "Iglu Central - Mirror 01"))
              
              config.outputEnrichedTsv must beFalse
          }
        }
    }
  }

  private def load(args: List[String]): IO[Either[String, MicroConfig]] = {
    Command("test-app", "test")(Configuration.load()).parse(args).right.get.value
  }
}
