/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2018-2020 Snowplow Analytics Ltd. All rights reserved.
  */

import sbt._

object Dependencies {

  val resolvers = Seq(
    "Snowplow Bintray" at "https://snowplow.bintray.com/snowplow-maven",
    "Snowplow Maven " at "http://maven.snplow.com/releases"
  )

  object V {
    // Snowplow
    val snowplowStreamCollector = "1.0.1"
    val snowplowCommonEnrich    = "1.3.0"

    // circe
    val circe = "0.11.1"

    // specs2
    val specs2        = "4.9.4"
  }

  // Snowplow stream collector
  val snowplowStreamCollector = "com.snowplowanalytics" %% "snowplow-stream-collector-core" % V.snowplowStreamCollector
  val snowplowCommonEnrich    = "com.snowplowanalytics" %% "snowplow-common-enrich"         % V.snowplowCommonEnrich
   
  // circe
  val circeJawn    = "io.circe" %% "circe-jawn"    % V.circe
  val circeGeneric = "io.circe" %% "circe-generic" % V.circe

  // specs2
  val specs2       = "org.specs2"    %% "specs2-core"   % V.specs2       % Test
}
