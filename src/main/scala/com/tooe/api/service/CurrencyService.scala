package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.CurrencyActor
import akka.pattern.ask

class CurrencyService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import CurrencyService._

  lazy val currencyActor = lookup(CurrencyActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route = (mainPrefix & pathPrefix(Root)) {
    routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          authenticateBySession {
            s: UserSession =>
              complete(currencyActor.ask(CurrencyActor.GetAllCurrencies(routeContext.lang)).mapTo[SuccessfulResponse])
          }
        }
      }
  }

}

object CurrencyService {
  val Root = "currencies"
}
