package com.tooe.core.usecase.photo_like

import akka.actor.Actor
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.domain.PhotoLike
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.service.PhotoLikeDataService
import com.tooe.core.domain.{UserId, PhotoId}
import akka.pattern.pipe
import com.tooe.core.util.ActorHelper

object PhotoLikeDataActor {
  final val Id = Actors.PhotoLikeDate

  case class Like(photoLike: PhotoLike)
  case class DeleteLike(photoId: PhotoId, userId: UserId)
  case class GetLikes(photoId: PhotoId, offset: Int = 0, limit: Int = 20)
  case class UserLikeExist(userId: UserId, photoId: PhotoId)
  case class UserPhotosLikes(userId: UserId, photoIds: Seq[PhotoId])

}

class PhotoLikeDataActor extends Actor with AppActors with DefaultTimeout {

  import PhotoLikeDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PhotoLikeDataService]

  def receive = {

    case Like(like) =>
       Future { service.save(like) } pipeTo sender

    case DeleteLike(photoId, userId) =>
      Future { service.delete(photoId, userId) } pipeTo sender

    case GetLikes(photoId, offset, limit) =>
      Future { service.getLikesByPhoto(photoId, offset, limit) } pipeTo sender

    case UserLikeExist(userId, photoId) =>  Future { service.userLikeExist(photoId, userId) } pipeTo sender

    case UserPhotosLikes(userId, photoIds) => Future { service.userPhotosLikes(photoIds, userId) } pipeTo sender

  }

}
