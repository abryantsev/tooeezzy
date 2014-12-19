package com.tooe.core.usecase.location_news

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.domain.LocationNewsLike
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationNewsLikeDataService
import scala.concurrent.Future
import com.tooe.core.domain.{LocationNewsId, UserId}
import akka.pattern._
import com.tooe.api.service.OffsetLimit


object LocationNewsLikeDataActor {
  final val Id = Actors.LocationNewsLikeData

  case class SaveLike(like: LocationNewsLike)
  case class DeleteLike(userId: UserId, locationNewsId: LocationNewsId)
  case class GetUserLocationNewsLikesByNews(newsIds: Seq[LocationNewsId], userId: UserId)
  case class GetLastLikes(locationNewsId: LocationNewsId)
}

class LocationNewsLikeDataActor extends Actor with DefaultTimeout with AppActors {

  import LocationNewsLikeDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[LocationNewsLikeDataService]

  def receive = {

    case SaveLike(like) => Future { service.save(like) } pipeTo sender

    case GetUserLocationNewsLikesByNews(newsIds, userId) => Future { service.getLikesByUserAndNews(newsIds, userId) } pipeTo sender

    case DeleteLike(userId, locationNewsId) => Future { service.deleteLike(userId, locationNewsId) } pipeTo sender

    case GetLastLikes(locationNewsId) => Future { service.getLikes(locationNewsId, OffsetLimit(0, 10)) } pipeTo sender
  }

}
