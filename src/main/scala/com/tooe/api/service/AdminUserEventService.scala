package com.tooe.api.service

import akka.actor.ActorSystem
import akka.pattern._
import spray.routing.PathMatcher
import com.tooe.core.usecase.admin_user_event.{AdminUserEventWriteActor, AdminUserEventReadActor}
import com.tooe.core.domain.{AdminRoleId, AdminUserEventId}
import com.tooe.core.usecase.admin_user_event.AdminUserEventWriteActor.DeleteEvent
import com.tooe.core.db.mongo.domain.AdminRole

class AdminUserEventService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {
  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserEventService._

  lazy val adminUserEventReadActor = lookup(AdminUserEventReadActor.Id)
  lazy val adminUserEventWriteActor = lookup(AdminUserEventWriteActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) { routeContext: RouteContext =>
    authenticateAdminBySession { _: AdminUserSession =>
    pathEndOrSingleSlash {
      get {
        authenticateAdminBySession { implicit adminSession: AdminUserSession =>
          authorizeByRole(AdminRoleId.Client) {
            complete((adminUserEventReadActor ? AdminUserEventReadActor.GetEventByUser(adminSession.adminUserId)).mapTo[SuccessfulResponse])
          }
        }
      }
    } ~
    path(Segment).as(AdminUserEventId) { adminEventId: AdminUserEventId =>
      authenticateAdminBySession { implicit adminSession: AdminUserSession =>
        authorizeByRole(AdminRoleId.Client) {
          delete {
            complete((adminUserEventWriteActor ? DeleteEvent(adminSession.adminUserId, adminEventId)).mapTo[SuccessfulResponse])
          }
        }
      }
    }
    }
  }

}

object AdminUserEventService {
  val Root = PathMatcher("admevents")
}