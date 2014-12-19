package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.CalendarDates
import java.util.Date

trait CalendarDatesConverter {

  import DBObjectConverters._

  implicit val calendarDatesConverter = new DBObjectConverter[CalendarDates] {

    def serializeObj(obj: CalendarDates) = DBObjectBuilder()
      .field("d").value(obj.date)
      .field("t").value(obj.time)

    def deserializeObj(source: DBObjectExtractor) = CalendarDates(
      date = source.field("d").value[Date],
      time = source.field("t").opt[Date]
    )

  }

}
