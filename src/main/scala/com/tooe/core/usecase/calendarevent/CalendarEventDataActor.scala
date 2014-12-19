package com.tooe.core.usecase.calendarevent

import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.CalendarEvent
import com.tooe.core.domain.{UserId, CalendarEventId}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CalendarEventDataService
import com.tooe.core.usecase.AppActor
import scala.concurrent.Future
import com.tooe.api.service.ExecutionContextProvider

object CalendarEventDataActor {
  
  val Id = Actors.CalendarEventData

  case class Save(event: CalendarEvent)
  case class Update(eventId: CalendarEventId, request: CalendarEventChangeRequest)
  case class Delete(eventId: CalendarEventId)
  case class SearchEvents(userId: UserId, request: SearchCalendarEventsRequest)
  case class SearchEventsCount(userId: UserId, request: SearchCalendarEventsRequest)

}

class CalendarEventDataActor extends AppActor with ExecutionContextProvider{

  import CalendarEventDataActor._

  lazy val service = BeanLookup[CalendarEventDataService]

  def receive = {
    case Save(event) => Future { service.save(event) } pipeTo sender
    case Update(eventId, request) => Future { service.update(eventId, request) }
    case Delete(eventId) => Future { service.delete(eventId) }
    case SearchEvents(userId, request) => Future { service.searchEvents(userId, request) } pipeTo sender
    case SearchEventsCount(userId, request) => Future { service.searchEventsCount(userId, request) } pipeTo sender
  }
}
