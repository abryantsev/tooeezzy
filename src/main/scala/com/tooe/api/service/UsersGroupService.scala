package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase._
import akka.pattern.ask

class UsersGroupService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import UsersGroupService._

  lazy val userGroupActor = lookup(UsersGroupActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(userGroupActor.ask(UsersGroupActor.GetAllUsersGroups(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }

}

object UsersGroupService {
  val Root = "usergroups"
}
