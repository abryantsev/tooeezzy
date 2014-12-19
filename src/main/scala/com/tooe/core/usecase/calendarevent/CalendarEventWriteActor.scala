package com.tooe.core.usecase.calendarevent

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service.{OffsetLimit, SuccessfulResponse}
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.{CalendarDates, CalendarEvent}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain.{Unsetable, CalendarEventId, UserId}
import com.tooe.core.usecase.AppActor
import java.util.Date

object CalendarEventWriteActor {

  val Id = Actors.CalendarEventWrite

  case class AddEvent(userId: UserId, request: CalendarEventAddRequest)
  case class ChangeEvent(eventId: CalendarEventId, request: CalendarEventChangeRequest)
  case class DeleteEvent(eventId: CalendarEventId)

}

class CalendarEventWriteActor extends AppActor {

  import CalendarEventWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val calendarEventDataActor = lookup(CalendarEventDataActor.Id)

  def receive = {
    case AddEvent(uId, request) =>
      val event = CalendarEvent(userId = uId, name = request.name, description = request.description, dates = CalendarDates(request.date, request.time))
      (calendarEventDataActor ? CalendarEventDataActor.Save(event)).mapTo[CalendarEvent].map { event =>
        CalendarEventSaveResponse(CalendarEventIdResponse(event.id))
      } pipeTo sender

    case ChangeEvent(eventId, request) =>
      calendarEventDataActor ! CalendarEventDataActor.Update(eventId, request)
      sender ! SuccessfulResponse

    case DeleteEvent(eventId) =>
      calendarEventDataActor ! CalendarEventDataActor.Delete(eventId)
      sender ! SuccessfulResponse
  }

}

case class CalendarEventAddRequest(name: String, description: Option[String], date: Date, time: Option[Date]) extends UnmarshallerEntity

case class CalendarEventChangeRequest(name: Option[String], description: Unsetable[String], date: Option[Date], time: Unsetable[Date]) extends UnmarshallerEntity

case class CalendarEventSaveResponse(@JsonProperty("calendarevent") calendarEvent: CalendarEventIdResponse) extends SuccessfulResponse

case class CalendarEventIdResponse(id: CalendarEventId)