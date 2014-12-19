package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter

@ReadingConverter
class OptionReadConverter[T] extends Converter[T, Option[T]] {

  def convert(source: T): Option[T] = {
    Some(source)
  }
}
