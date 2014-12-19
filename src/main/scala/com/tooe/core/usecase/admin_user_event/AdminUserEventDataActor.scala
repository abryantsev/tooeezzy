package com.tooe.core.usecase.admin_user_event

import com.tooe.core.application.Actors
import com.tooe.core.domain.{AdminUserEventId, AdminUserId}
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.AdminUserEventDataService
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.AdminUserEvent


object AdminUserEventDataActor {
  final val Id = Actors.AdminEventData

  case class SaveAdminEvent(event: AdminUserEvent)
  case class GetEventByUser(userId: AdminUserId)
  case class DeleteAdminEvent(userId: AdminUserId, eventId: AdminUserEventId)
  case class DeleteAdminEvenByUser(userId: AdminUserId)
}

class AdminUserEventDataActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserEventDataActor._

  lazy val service = BeanLookup[AdminUserEventDataService]

  def receive = {

    case SaveAdminEvent(event) => Future { service.save(event) }  pipeTo sender
    case GetEventByUser(userId) => Future { service.findByUser(userId) } pipeTo sender
    case DeleteAdminEvent(userId, eventId) => Future { service.delete(userId, eventId) }
    case DeleteAdminEvenByUser(userId) => Future { service.deleteByUser(userId) }

  }

}
