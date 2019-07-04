/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
  */

import sbt._
import Keys._

// SBT Assembly
import sbtassembly._
import sbtassembly.AssemblyKeys._

object Settings {

  lazy val compilerOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-target:jvm-1.8",
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
    "-source", "1.8",
    "-target", "1.8",
    "-Xlint"
  )

  lazy val assemblyOptions = Seq(
    assembly / target := file("target/scala-2.11/assembled_jars/"),
    assembly / assemblyJarName := { name.value + "-" + version.value + ".jar" },
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )
}
