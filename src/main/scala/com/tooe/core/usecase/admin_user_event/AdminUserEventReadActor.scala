package com.tooe.core.usecase.admin_user_event

import com.tooe.core.application.Actors
import com.tooe.core.domain.{AdminUserEventId, AdminUserId}
import com.tooe.core.usecase.AppActor
import akka.pattern._
import com.tooe.core.db.mongo.domain.AdminUserEvent
import com.tooe.api.service.SuccessfulResponse
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date

object AdminUserEventReadActor {
  final val Id = Actors.AdminEventRead

  case class GetEventByUser(userId: AdminUserId)
}

class AdminUserEventReadActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserEventReadActor._

  lazy val adminEventDataActor = lookup(AdminUserEventDataActor.Id)

  def receive = {

    case GetEventByUser(userId) =>
      (adminEventDataActor ? AdminUserEventDataActor.GetEventByUser(userId)).mapTo[Seq[AdminUserEvent]].map { events: Seq[AdminUserEvent] =>
        AdminUserEventsResponse(events map (AdminUserEventItem(_)))
      } pipeTo sender

  }

}

case class AdminUserEventsResponse(@JsonProperty("admevents") events: Seq[AdminUserEventItem]) extends SuccessfulResponse

case class AdminUserEventItem(id: AdminUserEventId, time: Date,  message: String)

object AdminUserEventItem {

  def apply(event: AdminUserEvent): AdminUserEventItem =
    AdminUserEventItem(event.id, event.createdTime, event.message)

}