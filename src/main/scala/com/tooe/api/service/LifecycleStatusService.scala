package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.LifecycleStatusActor
import akka.pattern.ask

class LifecycleStatusService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import LifecycleStatusService._

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val lifecycleStatusActor = lookup(LifecycleStatusActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(lifecycleStatusActor.ask(LifecycleStatusActor.GetAllLifecycleStatuses(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }


}

object LifecycleStatusService {
  val Root = "lifecyclestatuses"
}
