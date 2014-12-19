package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter
import com.tooe.core.db.mongo.domain.WishPresentStatus

@WritingConverter
class WishPresentStatusWriteConverter extends Converter[WishPresentStatus, String] {

  override def convert(source: WishPresentStatus) = source.id
}