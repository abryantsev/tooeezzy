package com.tooe.api.service

import com.tooe.core.usecase.ModerationStatusActor
import akka.actor.ActorSystem
import akka.pattern.ask


class ModerationStatusService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

    import ModerationStatusService._

    lazy val moderationStatusActor = lookup(ModerationStatusActor.Id)

    implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

    val route = (mainPrefix & pathPrefix(Root)) {
      routeContext: RouteContext =>
        pathEndOrSingleSlash {
          get {
            authenticateBySession {
              s: UserSession =>
                complete(moderationStatusActor.ask(ModerationStatusActor.GetAllModerationStatues(routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
    }

}

object ModerationStatusService {
  val Root = "moderationstatuses"
}
