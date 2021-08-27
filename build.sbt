/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2019-2021 Snowplow Analytics Ltd. All rights reserved.
  */
lazy val root = project
  .in(file("."))
  .settings(
    name := "snowplow-micro",
    organization := "com.snowplowanalytics.snowplow",
    description := "Standalone application to automate testing of trackers",
    scalaVersion := "2.12.14",
    scalacOptions := Settings.compilerOptions,
    javacOptions := Settings.javaCompilerOptions,
    resolvers ++= Dependencies.resolvers
  )
  .settings(Settings.assemblyOptions)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.snowplowStreamCollector,
      Dependencies.snowplowCommonEnrich,
      Dependencies.circeJawn,
      Dependencies.circeGeneric,
      Dependencies.specs2,
      Dependencies.thrift,
      Dependencies.sprayJson,
      Dependencies.jackson,
      Dependencies.badRows
    )
  )
  .settings(excludeDependencies ++= Dependencies.exclusions)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      scalaVersion),
    buildInfoPackage := "buildinfo"
  )
  .settings(Settings.dynverOptions)

import com.typesafe.sbt.packager.docker._
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
Docker / packageName := "snowplow/snowplow-micro"
Docker / maintainer := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>"
dockerBaseImage := "adoptopenjdk:11-jre-hotspot-focal"
Docker / daemonUser := "daemon"
dockerUpdateLatest := true

scriptClasspath += "/config"
