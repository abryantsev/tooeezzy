package com.tooe.core.usecase

import com.tooe.api.service.{SuccessfulResponse, ExecutionContextProvider}
import com.tooe.core.application.Actors
import com.tooe.core.domain.{PromotionStatus, UserId, PromotionId}
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.{Promotion, PromotionVisitor}
import com.tooe.core.usecase.promotion.{PromotionDataActor, PromotionVisitorDataActor}
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.util.DateHelper

object PromotionVisitorWriteActor {
  final val Id = Actors.PromotionVisitorWrite

  case class GoingToPromotion(promotion: PromotionId, userId: UserId, status: PromotionStatus)
}

class PromotionVisitorWriteActor extends AppActor with ExecutionContextProvider {

  lazy val promotionDataActor = lookup(PromotionDataActor.Id)
  lazy val promotionVisitorDataActor = lookup(PromotionVisitorDataActor.Id)

  import PromotionVisitorWriteActor._

  def receive = {
    case GoingToPromotion(promotionId, userId, status) =>
      getPromotion(promotionId) map { _ =>
        findPromotionVisitor(promotionId, userId) onSuccess {
          case Some(pv) if pv.status == status => //do nothing
          case _                               => promotionVisitorStatusHasChanged(promotionId, userId, status)
        }
        SuccessfulResponse
      } pipeTo sender
  }

  def getPromotion(id: PromotionId): Future[Promotion] =
    (promotionDataActor ? PromotionDataActor.GetPromotion(id)).mapTo[Promotion]

  def findPromotionVisitor(promotionId: PromotionId, userId: UserId): Future[Option[PromotionVisitor]] =
    (promotionVisitorDataActor ? PromotionVisitorDataActor.Find(promotionId, userId)).mapTo[Option[PromotionVisitor]]

  def promotionVisitorStatusHasChanged(promotionId: PromotionId, userId: UserId, status: PromotionStatus): Unit = {
    updateStatus(promotionId, userId, status)
    val delta = status match {
      case PromotionStatus.Confirmed => 1
      case PromotionStatus.Rejected  => -1
    }
    promotionDataActor ! PromotionDataActor.IncrementVisitorsCounter(promotionId, delta)
  }

  def updateStatus(promotionId: PromotionId, userId: UserId, status: PromotionStatus): Future[UpdateResult] =
    (promotionVisitorDataActor ? PromotionVisitorDataActor.UpsertStatus(promotionId, userId, DateHelper.currentDate, status)).mapTo[UpdateResult]
}