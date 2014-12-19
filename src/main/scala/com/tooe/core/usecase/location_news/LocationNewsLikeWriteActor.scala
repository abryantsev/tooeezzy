package com.tooe.core.usecase.location_news

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.domain.{UserId, LocationNewsId}
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.api.service.SuccessfulResponse
import akka.pattern._
import com.tooe.core.usecase.location_news.LocationNewsLikeDataActor.SaveLike
import com.tooe.core.db.mongo.domain.LocationNewsLike

object LocationNewsLikeWriteActor {
  final val Id = Actors.LocationNewsLikeWrite

  case class LikeLocationNews(locationNewsId: LocationNewsId, userId: UserId)
  case class UnlikeLocationNews(userId: UserId, locationNewsId: LocationNewsId)

}

class LocationNewsLikeWriteActor extends Actor with DefaultTimeout with AppActors {

  import LocationNewsLikeWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val locationNewsLikeDataActor = lookup(LocationNewsLikeDataActor.Id)
  lazy val locationNewsDataActor = lookup(LocationNewsDataActor.Id)

  def receive = {
    case LikeLocationNews(locationNewsId, userId) =>
      (locationNewsLikeDataActor ? SaveLike(LocationNewsLike(locationNewsId = locationNewsId, userId = userId))).onSuccess {
        case _ =>
          locationNewsDataActor ! LocationNewsDataActor.LikeLocationNews(userId, locationNewsId)
      }

    case UnlikeLocationNews(userId, locationNewsId) =>
      (locationNewsLikeDataActor ? LocationNewsLikeDataActor.DeleteLike(userId, locationNewsId)) pipeTo sender


  }

}
