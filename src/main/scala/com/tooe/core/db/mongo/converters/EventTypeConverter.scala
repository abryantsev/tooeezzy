package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import com.tooe.core.db.mongo.domain.EventType
import com.mongodb.DBObject
import com.tooe.core.domain.EventTypeId
import org.springframework.core.convert.converter.Converter

@WritingConverter
class EventTypeWriteConverter extends Converter[EventType, DBObject] with EventTypeConverter{
  def convert(source: EventType) = eventTypeConverter.serialize(source)
}

@ReadingConverter
class EventTypeReadConverter extends Converter[DBObject, EventType] with EventTypeConverter {
  def convert(source: DBObject) = eventTypeConverter.deserialize(source)
}

trait EventTypeConverter {

  import DBObjectConverters._

  implicit val eventTypeConverter = new DBObjectConverter[EventType] {
    def serializeObj(obj: EventType) = DBObjectBuilder()
      .id.value(obj.id)
      .field("eg").value(obj.eventGroups)
      .field("n").value(obj.name)
      .field("m").value(obj.message)
      .field("um").value(obj.userEventMessage)

    def deserializeObj(source: DBObjectExtractor) = EventType(
      id = source.id.value[EventTypeId],
      eventGroups = source.field("eg").seq[String],
      name = source.field("n").objectMap[String],
      message = source.field("m").objectMap[String],
      userEventMessage = source.field("um").objectMap[String]
    )
  }
}
