/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2019-2019 Snowplow Analytics Ltd. All rights reserved.
  */
lazy val root = project
  .in(file("."))
  .settings(
    name := "snowplow-micro",
    organization := "com.snowplowanalytics",
    version := "0.1.0-SNAPSHOT",
    description := "Receives payloads from trackers, validates them, stores them in-memory and offers REST API to query them",
    scalaVersion := "2.11.12",
    scalacOptions := Settings.compilerOptions,
    javacOptions := Settings.javaCompilerOptions
  )
  .settings(Settings.assemblyOptions)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.snowplowStreamCollector,
      Dependencies.circeJawn,
      Dependencies.circeGeneric,
      Dependencies.specs2,
      Dependencies.scalazSpecs2
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

enablePlugins(DockerPlugin)
packageName in Docker := "snowplow-micro"
version in Docker := version.value
maintainer in Docker := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>"
dockerBaseImage := "openjdk:8"
dockerRepository := Some("snowplow-docker-registry.bintray.io")
dockerUsername := Some("snowplow")
dockerUpdateLatest := true
