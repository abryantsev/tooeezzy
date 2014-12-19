package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.OnlineStatusActor
import akka.pattern.ask

class OnlineStatusService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import OnlineStatusService._

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val onlineStatusActor = lookup(OnlineStatusActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(onlineStatusActor.ask(OnlineStatusActor.GetAllOnlineStatuses(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }

}

object OnlineStatusService {
  val Root = "onlinestatuses"
}
