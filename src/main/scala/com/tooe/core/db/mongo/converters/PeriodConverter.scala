package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.{PeriodId}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import com.tooe.core.db.mongo.domain.Period

@WritingConverter
class PeriodWriteConverter extends Converter[Period, DBObject] with PeriodConverter {

  def convert(source: Period) = periodConverter.serialize(source)

}

@ReadingConverter
class PeriodReadConverter extends Converter[DBObject, Period] with PeriodConverter {

  def convert(source: DBObject) = periodConverter.deserialize(source)

}

trait PeriodConverter {

  import DBObjectConverters._

  implicit val periodConverter = new DBObjectConverter[Period] {
    def serializeObj(obj: Period) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = Period(
      id = source.id.value[PeriodId],
      name = source.field("n").objectMap[String]
    )
  }

}
