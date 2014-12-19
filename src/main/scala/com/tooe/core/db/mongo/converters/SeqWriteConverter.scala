package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import java.util

import scala.collection.JavaConverters._

@WritingConverter
class SeqWriteConverter extends Converter[Seq[Any], util.ArrayList[Any]] {

  override def convert(source: Seq[Any]) = new util.ArrayList(source.asJavaCollection)
}
