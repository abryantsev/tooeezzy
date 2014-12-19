package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{CalendarDates, CalendarEvent}
import com.tooe.core.domain.{UserId, CalendarEventId}
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class CalendarEventWriteConverter extends Converter[CalendarEvent, DBObject] with CalendarEventConverter {
  def convert(source: CalendarEvent): DBObject = calendarEventConverter.serialize(source)
}

@ReadingConverter
class CalendarEventReadConverter extends Converter[DBObject, CalendarEvent] with CalendarEventConverter {
  def convert(source: DBObject): CalendarEvent = calendarEventConverter.deserialize(source)
}

trait CalendarEventConverter extends CalendarDatesConverter {

  import DBObjectConverters._

  implicit val calendarEventConverter = new DBObjectConverter[CalendarEvent] {

    def serializeObj(obj: CalendarEvent) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("n").value(obj.name)
      .field("d").value(obj.description)
      .field("ds").value(obj.dates)

    def deserializeObj(source: DBObjectExtractor) = CalendarEvent(
      id = source.id.value[CalendarEventId],
      userId = source.field("uid").value[UserId],
      name = source.field("n").value[String],
      description = source.field("d").opt[String],
      dates = source.field("ds").value[CalendarDates]
    )

  }

}
