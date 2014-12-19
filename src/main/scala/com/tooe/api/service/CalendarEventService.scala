package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.core.domain.CalendarEventId
import com.tooe.core.usecase.calendarevent._
import spray.http.StatusCodes

class CalendarEventService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import CalendarEventService._

  lazy val calendarEventWriteActor = lookup(CalendarEventWriteActor.Id)
  lazy val calendarEventReadActor = lookup(CalendarEventReadActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
    pathEndOrSingleSlash {
      post {
        entity(as[CalendarEventAddRequest]) { request: CalendarEventAddRequest =>
          authenticateBySession { userSession: UserSession =>
              complete(StatusCodes.Created, (calendarEventWriteActor ? CalendarEventWriteActor.AddEvent(userSession.userId, request)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    path(ObjectId).as(CalendarEventId) { eventId: CalendarEventId =>
      post {
        entity(as[CalendarEventChangeRequest]) { request: CalendarEventChangeRequest =>
          authenticateBySession { _: UserSession =>
            complete((calendarEventWriteActor ? CalendarEventWriteActor.ChangeEvent(eventId, request)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    path(ObjectId).as(CalendarEventId) { eventId: CalendarEventId =>
      delete {
        authenticateBySession { _: UserSession =>
            complete((calendarEventWriteActor ? CalendarEventWriteActor.DeleteEvent(eventId)).mapTo[SuccessfulResponse])
        }
      }
    } ~
    path("search") {
      (parameters('name ?, 'starttime ?, 'endtime ?) & offsetLimit).as(SearchCalendarEventsRequest) { request: SearchCalendarEventsRequest =>
          get {
            authenticateBySession { userSession: UserSession =>
              complete((calendarEventReadActor ? CalendarEventReadActor.SearchEvents(userSession.userId, request)).mapTo[SuccessfulResponse])
            }
          }
      }
    }
  }
}

object CalendarEventService {
  val Root = "calendarevents"
}