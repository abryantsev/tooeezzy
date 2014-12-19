package com.tooe.core.usecase.promotion

import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.application.Actors
import concurrent.Future
import akka.pattern.pipe
import com.tooe.core.service.PromotionDataService
import com.tooe.core.usecase.{PromotionChangeRequest, AppActor, SearchPromotionsRequest}
import com.tooe.core.db.mongo.domain.Promotion
import com.tooe.core.domain.PromotionId
import com.tooe.core.util.Lang

object PromotionDataActor {
  final val Id = Actors.PromotionData

  case class SavePromotion(entity: Promotion)
  case class GetPromotion(id: PromotionId)

  case class SearchPromotions(request: SearchPromotionsRequest, lang: Lang)
  case class SearchPromotionsCount(request: SearchPromotionsRequest, lang: Lang)

  case class FindPromotions(ids: Set[PromotionId])
  case class FindPromotion(id: PromotionId)

  case class IncrementVisitorsCounter(id: PromotionId, amount: Int)

  case class UpdatePromotion(id: PromotionId, request: PromotionChangeRequest, lang: Lang)
}

class PromotionDataActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  lazy val service = BeanLookup[PromotionDataService]

  import com.tooe.core.usecase._
  import PromotionDataActor._

  def receive = {
    case SavePromotion(entity) => Future { service.save(entity) } pipeTo sender

    case GetPromotion(id) => Future { service.findOne(id) getOrNotFound (id, "Promotion") } pipeTo sender

    case IncrementVisitorsCounter(promotionId, delta) => service.incrementVisitorsCounter(promotionId, delta)

    case SearchPromotions(request, lang) => Future(service.searchPromotions(request, lang)) pipeTo sender

    case SearchPromotionsCount(request, lang) => Future(service.searchPromotionsCount(request, lang)) pipeTo sender

    case FindPromotions(ids) => Future(service.find(ids)) pipeTo sender

    case FindPromotion(id) => Future(service.findOne(id)) pipeTo sender

    case UpdatePromotion(id, request, lang) => Future { service.update(id, request, lang) }
  }
}