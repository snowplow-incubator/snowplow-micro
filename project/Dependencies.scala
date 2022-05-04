/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2018-2021 Snowplow Analytics Ltd. All rights reserved.
  */

import sbt._

object Dependencies {

  val resolvers = Seq(
    ("Snowplow Maven" at "http://maven.snplow.com/releases/").withAllowInsecureProtocol(true)
  )

  object V {
    // Snowplow
    val snowplowStreamCollector = "2.6.0"
    val snowplowCommonEnrich    = "3.1.3"

    // circe
    val circe = "0.14.1"

    // specs2
    val specs2        = "4.12.2"

    // force versions of transitive dependencies
    val thrift    = "0.14.1"
    val sprayJson = "1.3.6"
    val jackson   = "2.10.5.1"
    val badRows   = "2.1.1"
  }

  val exclusions = Seq(
    "org.apache.tomcat.embed" % "tomcat-embed-core"
  )

  // Snowplow stream collector
  val snowplowStreamCollector = "com.snowplowanalytics" %% "snowplow-stream-collector-core" % V.snowplowStreamCollector
  val snowplowCommonEnrich    = "com.snowplowanalytics" %% "snowplow-common-enrich"         % V.snowplowCommonEnrich

  // circe
  val circeJawn    = "io.circe" %% "circe-jawn"    % V.circe
  val circeGeneric = "io.circe" %% "circe-generic" % V.circe

  // specs2
  val specs2       = "org.specs2"    %% "specs2-core"   % V.specs2       % Test

  // transitive
  val thrift           = "org.apache.thrift"                % "libthrift"               % V.thrift
  val sprayJson        = "io.spray"                        %% "spray-json"              % V.sprayJson
  val jackson          = "com.fasterxml.jackson.core"       % "jackson-databind"        % V.jackson
  val badRows          = "com.snowplowanalytics"           %% "snowplow-badrows"        % V.badRows
}
