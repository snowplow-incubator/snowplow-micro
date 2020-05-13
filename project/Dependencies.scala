/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2018-2019 Snowplow Analytics Ltd. All rights reserved.
  */

import sbt._

object Dependencies {

  object V {
    // Snowplow stream collector
    val snowplowStreamCollector = "0.15.0"

    // circe
    val circe = "0.11.1"

    // specs2
    val specs2        = "4.9.4"
    val scalazSpecs2  = "0.2"
  }

  // Snowplow stream collector
  val snowplowStreamCollector = "com.snowplowanalytics" %% "snowplow-stream-collector-core" % V.snowplowStreamCollector
   
  // circe
  val circeJawn    = "io.circe" %% "circe-jawn"    % V.circe
  val circeGeneric = "io.circe" %% "circe-generic" % V.circe

  // specs2
  val specs2       = "org.specs2"    %% "specs2-core"   % V.specs2       % Test
  val scalazSpecs2 = "org.typelevel" %% "scalaz-specs2" % V.scalazSpecs2 % Test
}
