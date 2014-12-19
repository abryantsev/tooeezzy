package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.EventGroup
import com.tooe.core.domain.EventGroupId
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject


@WritingConverter
class EventGroupWriteConverter extends Converter[EventGroup, DBObject] with EventGroupConverter {

  def convert(source: EventGroup) = eventGroupConverter.serialize(source)

}

@ReadingConverter
class EventGroupReadConverter extends Converter[DBObject, EventGroup] with EventGroupConverter {

  def convert(source: DBObject) = eventGroupConverter.deserialize(source)

}

trait EventGroupConverter {

  import DBObjectConverters._

  implicit val eventGroupConverter = new DBObjectConverter[EventGroup] {
    def serializeObj(obj: EventGroup) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = EventGroup(
      id = source.id.value[EventGroupId],
      name = source.field("n").objectMap[String]
    )
  }

}
