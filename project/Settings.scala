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
import sbt._
import Keys._

// SBT Assembly
import sbtassembly._
import sbtassembly.AssemblyKeys._

// SBT DynVer
import sbtdynver.DynVerPlugin.autoImport._

import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._

object Settings {

  lazy val licenseSettings = Seq(
    licenses += ("Snowplow Limited Use License Agreement", url("https://docs.snowplow.io/limited-use-license-1.1")),
    headerLicense := Some(HeaderLicense.Custom(
      """|Copyright (c) 2019-present Snowplow Analytics Ltd. All rights reserved.
         |
         |This software is made available by Snowplow Analytics, Ltd.,
         |under the terms of the Snowplow Limited Use License Agreement, Version 1.1
         |located at https://docs.snowplow.io/limited-use-license-1.1
         |BY INSTALLING, DOWNLOADING, ACCESSING, USING OR DISTRIBUTING ANY PORTION
         |OF THE SOFTWARE, YOU AGREE TO THE TERMS OF SUCH LICENSE AGREEMENT.
         |""".stripMargin
    )),
    headerMappings := headerMappings.value + (HeaderFileType.conf -> HeaderCommentStyle.hashLineComment),
    Compile / unmanagedResources += file("LICENSE.md")
  )

  lazy val compilerOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused",
    "-Ywarn-value-discard",
    "-Ypartial-unification"
  )

  lazy val javaCompilerOptions = Seq(
    "-source", "11",
    "-target", "11",
    "-Xlint"
  )

  lazy val assemblyOptions = Seq(
    assembly / target := file("target/scala-2.12/assembled_jars/"),
    assembly / assemblyJarName := { name.value + "-" + version.value + ".jar" },
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "reference.conf" => MergeStrategy.concat
      case x => MergeStrategy.first
    }
  )

  lazy val dynverOptions = Seq(
    ThisBuild / dynverSeparator := "-",    // to be compatible with docker
    ThisBuild / dynverTagPrefix := "micro-",
  )
}
