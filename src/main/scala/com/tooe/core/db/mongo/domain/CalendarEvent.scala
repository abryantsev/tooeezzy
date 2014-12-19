package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UserId, CalendarEventId}
import java.util.Date

@Document(collection = "calendarevents")
case class CalendarEvent(id: CalendarEventId = CalendarEventId(),
                         userId: UserId,
                         name: String,
                         description: Option[String],
                         dates: CalendarDates)

case class CalendarDates(date: Date, time: Option[Date])
