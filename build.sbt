/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2019-2020 Snowplow Analytics Ltd. All rights reserved.
  */
lazy val root = project
  .in(file("."))
  .settings(
    name := "snowplow-micro",
    organization := "com.snowplowanalytics.snowplow",
    version := "0.1.0",
    description := "Standalone application to automate testing of trackers",
    scalaVersion := "2.12.11",
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
      Dependencies.specs2
    )
  )
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      organization,
      name,
      version,
      "shortName" -> "snowplow-micro",
      scalaVersion),
    buildInfoPackage := "buildinfo"
  )

import com.typesafe.sbt.packager.docker._
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
packageName in Docker := "snowplow/snowplow-micro"
maintainer in Docker := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>"
dockerBaseImage := "snowplow-docker-registry.bintray.io/snowplow/base-debian:0.1.0"
daemonUser in Docker := "snowplow"
dockerUpdateLatest := true
