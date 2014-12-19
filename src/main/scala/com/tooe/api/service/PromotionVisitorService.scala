package com.tooe.api.service

import com.tooe.core.usecase._
import akka.pattern.ask
import akka.actor.ActorSystem
import com.tooe.core.domain.{PromotionVisitorsField, PromotionFields, ViewType, PromotionId}
import spray.routing.PathMatcher
import com.tooe.api.validation.ValidationHelper

class PromotionVisitorService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  lazy val writeActor = lookup(PromotionVisitorWriteActor.Id)
  lazy val readActor = lookup(PromotionVisitorReadActor.Id)

  import PromotionVisitorWriteActor._
  import PromotionVisitorReadActor._

  val route =
    (mainPrefix & path("promotions" / Segment / "visitors").as(PromotionId)) { (routeContext: RouteContext, promotionId: PromotionId) =>
      post {
        entity(as[ChangePromotionStatusRequest]) { request: ChangePromotionStatusRequest =>
          authenticateBySession { userSession: UserSession =>
            complete {
              (writeActor ? GoingToPromotion(promotionId, userSession.userId, request.status))
                .mapTo[SuccessfulResponse]
            }
          }
        }
      } ~
      get {
        (parameter('entities.as[CSV[PromotionVisitorsField]] ?) & offsetLimit) { (fields, offsetLimit) =>
          authenticateBySession { s: UserSession =>
            val request = GetPromotionVisitors(
              userId = s.userId,
              promotionId = promotionId,
              fieldsOpt= fields,
              offsetLimit = offsetLimit
            )
            ValidationHelper.checkObject(request)
            complete((readActor ? request).mapTo[SuccessfulResponse])
          }
        }
      }
    }
}