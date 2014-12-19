package com.tooe.api.service

import com.tooe.core.usecase._
import akka.pattern.ask
import akka.actor.ActorSystem
import com.tooe.core.domain.{AdminRoleId, PromotionFields, ViewType, PromotionId}
import spray.routing.PathMatcher
import com.tooe.api.validation.ValidationHelper
import com.tooe.core.db.mongo.domain.AdminRole
import spray.http.StatusCodes

class PromotionService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import PromotionService._

  lazy val promotionReadActor = lookup(PromotionReadActor.Id)
  lazy val promotionWriteActor = lookup(PromotionWriteActor.Id)

  val route =
    (mainPrefix & path(Path.Root)) { implicit routeContext: RouteContext =>
      post {
        entity(as[SavePromotionRequest]) { spr: SavePromotionRequest =>
          authenticateAdminBySession { implicit s: AdminUserSession =>
            authorizeByRole(AdminRoleId.Client) {
              complete(StatusCodes.Created, promotionWriteActor.ask(PromotionWriteActor.SavePromotion(spr, routeContext)).mapTo[SavePromotionResponse])
            }
          }
        }
      }
    } ~
    (mainPrefix & path(Path.Root / PathMatcher("search"))) { routeContext: RouteContext =>
      get {
        (parameters('region, 'category ?, 'name ?, 'date ?, 'sort ?, 'isfavorite ?, 'entities.as[CSV[PromotionFields]] ?) & offsetLimit)
          .as(SearchPromotionsRequest) { spr: SearchPromotionsRequest =>
          ValidationHelper.checkObject(spr)
          authenticateBySession { s: UserSession =>
            complete(promotionReadActor.ask(PromotionReadActor.SearchPromotions(spr, s.userId, routeContext.lang)).mapTo[SearchPromotionsResponse])
          }
        }
      }
    } ~
    (mainPrefix & path(Path.Root / Segment).as(PromotionId)) { (routeContext: RouteContext, promotionId: PromotionId) =>
      get {
        parameters('view.as[ViewType] ?) { viewType: Option[ViewType] =>
          authenticateBySession { userSession: UserSession =>
            complete( promotionReadActor.ask(PromotionReadActor.GetPromotion(GetPromotionRequest(promotionId, viewType), userSession.userId, routeContext)).mapTo[GetPromotionResponse])
          }
        }
      } ~
      post {
        entity(as[PromotionChangeRequest]) { request: PromotionChangeRequest =>
          authenticateAdminBySession { implicit s: AdminUserSession =>
            authorizeByRole(AdminRoleId.Client) {
              complete(promotionWriteActor.ask(PromotionWriteActor.ChangePromotion(promotionId, request, routeContext.lang)).mapTo[SuccessfulResponse])
            }
          }
        }
      }
    }
}

object PromotionService {
  object Path {
    val Root = PathMatcher("promotions")
  }
}
