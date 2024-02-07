package com.snowplowanalytics.snowplow.micro

import cats.implicits._
import com.monovore.decline.Opts

import java.nio.file.Path

object Cli {

  final case class Config(collector: Option[Path],
                          enrich: Option[Path],
                          iglu: Option[Path],
                          tsv: Boolean)

  private val collector = Opts.option[Path]("collector-config", "Path to HOCON collector configuration (optional)", "c", "collector-config.hocon").orNone
  private val enrich = Opts.option[Path]("enrich-config", "Path to HOCON enrich configuration (optional)", "e", "enrich-config.hocon").orNone
  private val iglu = Opts.option[Path]("iglu", "Path to HOCON Iglu configuration (optional)", "i", "iglu.hocon").orNone
  private val outputEnrichedTsv = Opts.flag("output-tsv", "Print events in TSV format to standard output", "t").orFalse

  val config: Opts[Config] = (collector, enrich, iglu, outputEnrichedTsv).mapN(Config.apply)
}