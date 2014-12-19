package com.tooe.core.usecase.admin_user_event

import com.tooe.core.usecase.AppActor
import akka.pattern._
import com.tooe.core.db.mongo.domain.AdminUserEvent
import com.tooe.core.application.Actors
import com.tooe.core.domain.{AdminUserEventId, AdminUserId}
import com.tooe.core.usecase.admin_user_event.AdminUserEventDataActor.DeleteAdminEvent
import com.tooe.api.service.SuccessfulResponse


object AdminUserEventWriteActor {
  final val Id = Actors.AdminEventWrite

  case class DeleteEvent(userId: AdminUserId, eventId: AdminUserEventId)

}

class AdminUserEventWriteActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserEventWriteActor._

  lazy val adminEventDataActor = lookup(AdminUserEventDataActor.Id)

  def receive = {

    case DeleteEvent(userId, eventId) =>
      adminEventDataActor ! DeleteAdminEvent(userId, eventId)
      sender ! SuccessfulResponse

  }

}





