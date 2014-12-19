package com.tooe.core.usecase.promotion

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.domain.{PromotionStatus, UserId, PromotionId}
import concurrent.Future
import com.tooe.api.service.{ExecutionContextProvider, OffsetLimit}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PromotionVisitorDataService
import java.util.Date

object PromotionVisitorDataActor {
  final val Id = Actors.PromotionVisitorData

  case class GetPromotionVisitors(promotionId: PromotionId, offsetLimit: OffsetLimit)
  case class CountPromotionVisitors(promotionId: PromotionId)
  case class FindVisitorUserIds(promotionId: PromotionId)
  case class UpsertStatus(promotionId: PromotionId, visitor: UserId, date: Date, status: PromotionStatus)
  case class Find(promotionId: PromotionId, userId: UserId)
  case class FindByPromotions(promotionIds: Set[PromotionId])
}

class PromotionVisitorDataActor extends AppActor with ExecutionContextProvider {

  lazy val visitorService = BeanLookup[PromotionVisitorDataService]

  import PromotionVisitorDataActor._

  def receive = {

    case GetPromotionVisitors(promotionId, offsetLimit) =>
      Future(visitorService.findAllVisitors(promotionId, offsetLimit)) pipeTo sender

    case CountPromotionVisitors(promotionId) =>
      Future(visitorService.countAllVisitors(promotionId)) pipeTo sender

    case FindVisitorUserIds(promotionId) =>
      Future(visitorService.findAllVisitorIds(promotionId)) pipeTo sender

    case UpsertStatus(promotionId, visitor, date, status) =>
      Future(visitorService.upsertStatus(promotionId, visitor, date, status)) pipeTo sender

    case Find(promotionId, userId) => Future(visitorService.find(promotionId, userId)) pipeTo sender

    case FindByPromotions(promotionIds) => Future(visitorService.findVisitors(promotionIds)) pipeTo sender
  }
}