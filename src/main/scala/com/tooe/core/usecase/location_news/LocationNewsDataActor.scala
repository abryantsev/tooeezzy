package com.tooe.core.usecase.location_news

import akka.actor.Actor
import akka.pattern.pipe
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.db.mongo.domain.LocationNews
import scala.concurrent.Future
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationNewsDataService
import com.tooe.core.domain.{LocationsChainId, UserId, LocationId, LocationNewsId}
import com.tooe.api.service.{OffsetLimit, ChangeLocationNewsRequest}
import com.tooe.core.util.Lang
import com.tooe.core.exceptions.NotFoundException

object LocationNewsDataActor {
  final val Id = Actors.LocationNewsData

  case class CreateLocationNews(locationNews: LocationNews)
  case class RemoveLocationNews(locationNewsId: LocationNewsId)
  case class UpdateLocationNews(locationNewsId: LocationNewsId, request: ChangeLocationNewsRequest, lang: Lang)
  case class GetLocationNews(locationId: LocationId, offsetLimit: OffsetLimit)
  case class GetLocationsChainNews(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit)
  case class GetLocationNewsCount(locationId: LocationId)
  case class GetLocationsChainNewsCount(locationsChainId: LocationsChainId)
  case class LikeLocationNews(userId: UserId, locationNewsId: LocationNewsId)
  case class UnlikeLocationNews(userIds: Seq[UserId], locationNewsId: LocationNewsId)
  case class FindLocationNews(id: LocationNewsId)
}

class LocationNewsDataActor extends Actor with DefaultTimeout with AppActors {

  import LocationNewsDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[LocationNewsDataService]

  def receive = {

    case CreateLocationNews(locationNews) => Future { service.save(locationNews) } pipeTo sender

    case RemoveLocationNews(locationNewsId) => Future { service.remove(locationNewsId) }

    case UpdateLocationNews(locationNewsId, request, lang) => Future { service.update(locationNewsId, request, lang) }

    case GetLocationNews(locationId, offsetLimit) => Future { service.getLocationNews(locationId, offsetLimit) } pipeTo sender

    case GetLocationNewsCount(locationId) => Future { service.getLocationNewsCount(locationId) } pipeTo sender

    case GetLocationsChainNews(locationsChainId, offsetLimit) => Future { service.getLocationsChainNews(locationsChainId, offsetLimit) } pipeTo sender

    case GetLocationsChainNewsCount(locationsChainId) => Future { service.getLocationsChainNewsCount(locationsChainId) } pipeTo sender

    case LikeLocationNews(userId: UserId, locationNewsId: LocationNewsId) => Future { service.updateUserLikes(locationNewsId, userId) }

    case UnlikeLocationNews(userIds, locationNewsId) => Future { service.updateUserUnlikes(locationNewsId, userIds.toList) }

    case FindLocationNews(id: LocationNewsId) => Future { service.findOne(id) getOrElse NotFoundException("News not found") }
  }

}
