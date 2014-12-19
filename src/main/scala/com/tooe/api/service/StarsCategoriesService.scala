package com.tooe.api.service

import spray.routing.PathMatcher
import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.core.usecase.{StarCategoriesRequest, StarsCategoriesActor}
import com.tooe.core.usecase.StarsCategoriesActor.GetCategories
import com.tooe.core.domain.StarCategoryField

class StarsCategoriesService (implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  import StarsCategoriesService._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val starsCategoriesActor = lookup(StarsCategoriesActor.Id)

  val route =
    (mainPrefix & pathPrefix(Root)) { implicit routeContext: RouteContext =>
      pathEndOrSingleSlash {
        get {
          parameters('fields.as[CSV[StarCategoryField]] ?).as(StarCategoriesRequest) { request: StarCategoriesRequest =>
            authenticateBySession { s: UserSession =>
              complete((starsCategoriesActor ? GetCategories(request, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }

}

object StarsCategoriesService {
  val Root = PathMatcher("starscategories")
}