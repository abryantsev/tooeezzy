package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import com.tooe.core.db.mongo.domain.WishPresentStatus

@ReadingConverter
class WishPresentStatusReadConverter extends Converter[String, WishPresentStatus] {
  override def convert(source: String) = WishPresentStatus.valueById(source)
}