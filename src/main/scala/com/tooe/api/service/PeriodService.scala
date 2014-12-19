package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.PeriodActor
import akka.pattern.ask

class PeriodService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import PeriodService._

  lazy val periodActor = lookup(PeriodActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(periodActor.ask(PeriodActor.GetAllPeriods(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }

}

object PeriodService {
  val Root = "periods"
}
