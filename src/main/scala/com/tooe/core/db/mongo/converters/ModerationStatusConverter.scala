package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.ModerationStatus
import com.tooe.core.domain.ModerationStatusId
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import org.springframework.data.convert.{ReadingConverter, WritingConverter}

@WritingConverter
class ModerationStatusWriteConverter extends Converter[ModerationStatus, DBObject] with ModerationStatusConverter {
  def convert(source: ModerationStatus) = moderationStatusConverter.serialize(source)
}

@ReadingConverter
class ModerationStatusReadConverter extends Converter[DBObject, ModerationStatus] with ModerationStatusConverter {
  def convert(source: DBObject) = moderationStatusConverter.deserialize(source)
}

trait ModerationStatusConverter {

  import DBObjectConverters._

  implicit val moderationStatusConverter = new DBObjectConverter[ModerationStatus] {
    def serializeObj(obj: ModerationStatus) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("d").value(obj.description)

    def deserializeObj(source: DBObjectExtractor) = ModerationStatus(
      id = source.id.value[ModerationStatusId],
      name = source.field("n").objectMap[String],
      description = source.field("d").objectMap[String]
    )
  }
}
