package com.tooe.core.usecase.calendarevent

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service.{SuccessfulResponse, ExecutionContextProvider, OffsetLimit}
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.{CalendarDates, CalendarEvent}
import com.tooe.core.domain.{UserId, CalendarEventId}
import com.tooe.core.usecase.AppActor
import java.util.Date
import com.tooe.api.validation.{ValidationContext, Validatable}
import scala.concurrent.Future
import com.tooe.core.util.DateHelper._
import org.joda.time.DateTime
import com.typesafe.config.ConfigFactory

object CalendarEventReadActor {

  val Id = Actors.CalendarEventRead

  case class SearchEvents(userId: UserId, request: SearchCalendarEventsRequest)

}

class CalendarEventReadActor extends AppActor with ExecutionContextProvider {

  import CalendarEventReadActor._

  lazy val calendarEventDataActor = lookup(CalendarEventDataActor.Id)

  def receive = {
    case SearchEvents(userId, request) =>
      (for{ 
        events <- calendarEventDataActor.ask(CalendarEventDataActor.SearchEvents(userId, request)).mapTo[Seq[CalendarEvent]]
        eventsCount <- if(request.offsetLimit.offset == 0) calendarEventDataActor.ask(CalendarEventDataActor.SearchEventsCount(userId, request)).mapTo[Long].map(c => Some(c)) else Future(None)
      } yield SearchCalendarEventsResponse(eventsCount, events.map(e => CalendarEventResponseItem(e)))
      ) pipeTo sender
  }

}

case class SearchCalendarEventsRequest
(
  name: Option[String] = None,
  @JsonProperty("starttime") startTime: Option[Date] = None,
  @JsonProperty("endtime") endTime: Option[Date] = None,
  offsetLimit: OffsetLimit
  ) extends Validatable {
  check

  def validate(ctx: ValidationContext): Unit = {

    if(name.isDefined && (startTime.isDefined || endTime.isDefined)) {
      ctx.fail("Wrong search parameters")
    }
    if(name.isDefined && name.get.length < 3) {
      ctx.fail("Wrong search parameters")
    }
    if(name.isEmpty & (startTime.isEmpty || endTime.isEmpty)) {
      ctx.fail("Wrong search parameters")
    }
    if(startTime.isDefined && endTime.isDefined && new DateTime(endTime.get).minusDays(SearchCalendarEventsConf.daysInterval).isAfter(startTime.get.getTime)){
      ctx.fail("Wrong search parameters")
    }
  }
}
object SearchCalendarEventsConf {

  val daysInterval = ConfigFactory.load().getInt("constraints.calendarevent.daysinterval")

}
case class CalendarEventResponseItem
(
  id: CalendarEventId,
  name: String,
  description: Option[String],
  dates:  CalendarDates
  )

object CalendarEventResponseItem {
  def apply(ce: CalendarEvent): CalendarEventResponseItem = CalendarEventResponseItem(
    id = ce.id,
    name = ce.name,
    description = ce.description,
    dates = ce.dates
  )
}

case class SearchCalendarEventsResponse
(
  @JsonProperty("calendareventscount") calendarEventsCount: Option[Long],
  @JsonProperty("calendarevents") calendarEvents: Seq[CalendarEventResponseItem]
  ) extends SuccessfulResponse
