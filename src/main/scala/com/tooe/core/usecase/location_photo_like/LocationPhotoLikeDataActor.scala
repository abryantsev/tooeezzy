package com.tooe.core.usecase.location_photo_like

import akka.actor.Actor
import akka.pattern.pipe
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.domain.LocationPhotoLike
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.service.LocationPhotoLikeDataService
import com.tooe.core.domain.{UserId, LocationPhotoId, LocationPhotoLikeId}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.UpdateUserLikes
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor

object LocationPhotoLikeDataActor {
  final val Id = Actors.LocationPhotoLikeData

  case class LikePhoto(like: LocationPhotoLike)
  case class DeleteLike(photoId: LocationPhotoId, userId: UserId)
  case class GetLastLikes(photoId: LocationPhotoId)
  case class UserLikeExist(photoId: LocationPhotoId, userId: UserId)
  case class GetLocationPhotoLikes(locationPhotoId: LocationPhotoId, offsetLimit: OffsetLimit)
  case class GetLocationPhotoLikesCount(locationPhotoId: LocationPhotoId)
  case class GetLocationPhotoLikesByPhotos(photoIds: Set[LocationPhotoId])
}

class LocationPhotoLikeDataActor extends Actor with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationPhotoLikeDataActor._

  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val service = BeanLookup[LocationPhotoLikeDataService]

  def receive = {

    case LikePhoto(like) => Future {
      service.save(like)
    }.onSuccess {
      case _ =>
        locationPhotoDataActor ! UpdateUserLikes(like.locationPhotoId, like.userId)
    }

    case DeleteLike(photoId, userId) => Future { service.delete(photoId, userId) } pipeTo sender

    case GetLastLikes(photoId) => Future { service.getPhotoLikes(photoId, OffsetLimit(0, 20)) } pipeTo sender

    case UserLikeExist(photoId, userId) => Future { service.userLikeExist(photoId, userId) } pipeTo sender

    case GetLocationPhotoLikes(locationPhotoId, offsetLimit) => Future { service.getPhotoLikes(locationPhotoId, offsetLimit) } pipeTo sender

    case GetLocationPhotoLikesCount(locationPhotoId) => Future { service.getPhotoLikesCount(locationPhotoId) } pipeTo sender

    case GetLocationPhotoLikesByPhotos(photoIds) => Future { service.getLikesByPhotos(photoIds) } pipeTo sender

  }

}
