package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class OptionWriteConverter[T] extends Converter[Option[T], T] {

  def convert(source: Option[T]): T = {
    source.asInstanceOf[Option[AnyRef]].orNull.asInstanceOf[T]
  }
}
