package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.usecase.{GetMaritalStatusesParams, GetLocationInfoRequest, MaritalStatusActor}
import akka.pattern.ask
import com.tooe.core.domain.{GenderType, ShowType}

class MaritalStatusService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import MaritalStatusService._

  lazy val maritalStatusActor = lookup(MaritalStatusActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route = (mainPrefix & pathPrefix(Root) & parameters('type.as[GenderType] ?).as(GetMaritalStatusesParams)) {
    (routeContext: RouteContext, params: GetMaritalStatusesParams) =>
      get {
        authenticateBySession {
          s: UserSession =>
            complete(maritalStatusActor.ask(MaritalStatusActor.FindByGender(params.gender, routeContext.lang)).mapTo[SuccessfulResponse])
        }
      }
  }

}

object MaritalStatusService {
  val Root = "maritalstatuses"
}
