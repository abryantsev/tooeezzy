package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.MaritalStatus
import com.tooe.core.domain.MaritalStatusId

@WritingConverter
class MaritalStatusWriteConverter extends Converter[MaritalStatus, DBObject] with MaritalStatusConverter {

  def convert(source: MaritalStatus) = MaritalStatusConverter.serialize(source)
}

@ReadingConverter
class MaritalStatusReadConverter extends Converter[DBObject, MaritalStatus] with MaritalStatusConverter {
  
  def convert(source: DBObject) = MaritalStatusConverter.deserialize(source)
}

trait MaritalStatusConverter {
  
  import DBObjectConverters._

  implicit val MaritalStatusConverter = new DBObjectConverter[MaritalStatus] {
    def serializeObj(obj: MaritalStatus) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("nf").value(obj.femaleStatusName)

    def deserializeObj(source: DBObjectExtractor) = MaritalStatus(
      id = source.id.value[MaritalStatusId],
      name = source.field("n").objectMap[String],
      femaleStatusName = source.field("nf").objectMap[String]
    )
  }
}