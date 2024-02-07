/**
  * PROPRIETARY AND CONFIDENTIAL
  *
  * Unauthorized copying of this file via any medium is strictly prohibited.
  *
  * Copyright (c) 2018-2022 Snowplow Analytics Ltd. All rights reserved.
  */

import sbt._

object Dependencies {

  val resolvers = Seq(
    ("Snowplow Maven" at "http://maven.snplow.com/releases/").withAllowInsecureProtocol(true)
  )

  object V {
    // Snowplow
    val snowplowStreamCollector = "2.8.1"
    val snowplowCommonEnrich    = "3.8.0"

    val decline          = "2.4.1"
    
    // circe
    val circe = "0.14.2"

    // specs2
    val specs2        = "4.12.2"

    // force versions of transitive dependencies
    val badRows   = "2.2.0"
  }

  val exclusions = Seq(
    "org.apache.tomcat.embed" % "tomcat-embed-core"
  )

  // Snowplow stream collector
  val snowplowStreamCollector = "com.snowplowanalytics" %% "snowplow-stream-collector-core" % V.snowplowStreamCollector
  val snowplowCommonEnrich    = "com.snowplowanalytics" %% "snowplow-common-enrich"         % V.snowplowCommonEnrich

   object Http4s{
     
     val decline           = "com.monovore"          %% "decline-effect"                        % V.decline
     val analyticsSdk      = "com.snowplowanalytics"            %% "snowplow-scala-analytics-sdk"           % "3.2.0" 
     val snowplowStreamCollector = "com.snowplowanalytics" %% "snowplow-stream-collector-http4s-core" % "3.1.0-2-4e8aecfc-20240202-1202" 
     val snowplowCommonEnrich    = "com.snowplowanalytics" %% "snowplow-enrich-common-fs2"         % "4.0.0-2-9664e317-SNAPSHOT" 
   }
 
  // circe
  val circeJawn    = "io.circe" %% "circe-jawn"    % V.circe
  val circeGeneric = "io.circe" %% "circe-generic" % V.circe

  // specs2
  val specs2       = "org.specs2"    %% "specs2-core"   % V.specs2       % Test

  // transitive
  val badRows          = "com.snowplowanalytics"           %% "snowplow-badrows"        % V.badRows
}
