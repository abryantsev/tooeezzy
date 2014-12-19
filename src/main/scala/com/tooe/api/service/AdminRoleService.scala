package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.AdminRoleActor
import akka.pattern.ask

class AdminRoleService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import AdminRoleService._

  lazy val adminRoleActor = lookup(AdminRoleActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(adminRoleActor.ask(AdminRoleActor.GetAllAdminRoles(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }

}

object AdminRoleService {
  val Root = "admuserroles"
}
