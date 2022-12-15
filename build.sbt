/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2019-2022 Snowplow Analytics Ltd. All rights reserved.
  */
import com.typesafe.sbt.packager.docker._

lazy val buildSettings = Seq(    
  name := "snowplow-micro",
  organization := "com.snowplowanalytics.snowplow",
  description := "Standalone application to automate testing of trackers",
  scalaVersion := "2.12.14",
  scalacOptions := Settings.compilerOptions,
  javacOptions := Settings.javaCompilerOptions,
  resolvers ++= Dependencies.resolvers
)

lazy val dependencies = Seq(
  libraryDependencies ++= Seq(
    Dependencies.snowplowStreamCollector,
    Dependencies.snowplowCommonEnrich,
    Dependencies.circeJawn,
    Dependencies.circeGeneric,
    Dependencies.specs2,
    Dependencies.badRows
  )
)

lazy val exclusions = Seq(
  excludeDependencies ++= Dependencies.exclusions
)

lazy val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](organization, name, version, scalaVersion),
  buildInfoPackage := "buildinfo"
)

lazy val dynVerSettings = Seq(
  ThisBuild / dynverVTagPrefix := false, // Otherwise git tags required to have v-prefix
  ThisBuild / dynverSeparator := "-"     // to be compatible with docker
)

lazy val commonSettings = 
  dependencies ++
  exclusions ++
  buildSettings ++
  buildInfoSettings ++
  dynVerSettings ++
  Settings.dynverOptions ++
  Settings.assemblyOptions

lazy val dockerCommon = Seq(
  Docker / maintainer := "Snowplow Analytics Ltd. <support@snowplow.io>",
  Docker / packageName := "snowplow/snowplow-micro",
  Docker / defaultLinuxInstallLocation := "/opt/snowplow",
  Docker / daemonUserUid := None,
  dockerRepository := Some("snowplow"),
  scriptClasspath += "/config",
)

lazy val microSettingsDistroless = dockerCommon ++ Seq(
  dockerBaseImage := "gcr.io/distroless/java11-debian11:nonroot",
  Docker / daemonUser := "nonroot",
  Docker / daemonGroup := "nonroot",
  dockerEntrypoint := Seq(
    "java",
    "-cp",
    s"/opt/snowplow/lib/${(packageJavaClasspathJar / artifactPath).value.getName}:/config",
    "com.snowplowanalytics.snowplow.micro.Main"
  ),
  dockerPermissionStrategy := DockerPermissionStrategy.CopyChown,
  sourceDirectory := (micro / sourceDirectory).value
)

lazy val microSettings = dockerCommon ++ Seq(
  dockerBaseImage := "eclipse-temurin:11",
  Docker / daemonUser := "daemon",
)

lazy val micro = project
  .in(file("."))
  .settings(commonSettings ++ microSettings)
  .enablePlugins(BuildInfoPlugin, DockerPlugin, JavaAppPackaging)
  
lazy val microDistroless = project
  .in(file("distroless/micro"))
  .settings(commonSettings ++ microSettingsDistroless)
  .enablePlugins(BuildInfoPlugin, DockerPlugin, JavaAppPackaging, ClasspathJarPlugin)
