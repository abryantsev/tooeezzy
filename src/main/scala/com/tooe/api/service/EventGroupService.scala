package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.EventGroupActor
import akka.pattern.ask

class EventGroupService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import EventGroupService._

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val eventGroupActor = lookup(EventGroupActor.Id)

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(eventGroupActor.ask(EventGroupActor.GetAllEventGroups(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }

}

object EventGroupService {

  val Root = "eventgroups"
}
