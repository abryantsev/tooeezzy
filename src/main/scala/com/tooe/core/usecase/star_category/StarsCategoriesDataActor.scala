package com.tooe.core.usecase.star_category

import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.StarsCategoriesDataService
import akka.pattern._
import scala.concurrent.Future
import akka.actor.Actor
import com.tooe.core.util.ActorHelper
import com.tooe.core.usecase.StarCategoriesRequest
import com.tooe.core.domain.StarCategoryId
import com.tooe.core.domain.StarCategoryId

object StarsCategoriesDataActor {
  final val Id = Actors.StarsCategoriesData

  case class GetCategories(request: StarCategoriesRequest)
  case class GetCategoriesBy(categoryIds: Seq[StarCategoryId])

  case class UpdateSubscribers(categoryId: StarCategoryId, delta: Int)
}

class StarsCategoriesDataActor extends Actor with ActorHelper {

  import StarsCategoriesDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[StarsCategoriesDataService]

  def receive = {
    case GetCategories(request) => Future(service.find(request.fields getOrElse Set())) pipeTo sender
    case GetCategoriesBy(categoryIds) => Future(service.findByIds(categoryIds)) pipeTo sender
    case UpdateSubscribers(categoryId, delta) => Future { service.updateSubscribers(categoryId, delta) }
  }
}