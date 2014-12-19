package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import java.util

import scala.collection.JavaConverters._

@ReadingConverter
class SeqReadConverter extends Converter[util.ArrayList[Any], Seq[Any]] {

  override def convert(source: util.ArrayList[Any]) = source.asScala.toSeq
}
