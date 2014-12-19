package com.tooe.core.usecase.location_photo_comment

import akka.actor.Actor
import akka.pattern.pipe
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.service.LocationPhotoCommentDataService
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.db.mongo.domain.LocationPhotoComment
import scala.concurrent.Future
import com.tooe.core.domain.LocationPhotoId
import com.tooe.api.service.OffsetLimit

object LocationPhotoCommentDataActor {
  final val Id = Actors.LocationPhotoCommentData

  case class AddComment(comment: LocationPhotoComment)
  case class GetLocationPhotoCommentsCount(photoId: LocationPhotoId)
  case class GetPhotoComments(locationPhotoId: LocationPhotoId, offsetLimit: OffsetLimit)

}

class LocationPhotoCommentDataActor extends Actor with AppActors with DefaultTimeout {
  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationPhotoCommentDataActor._

  lazy val service = BeanLookup[LocationPhotoCommentDataService]

  def receive = {

    case AddComment(comment) => Future { service.save(comment) } pipeTo sender

    case GetLocationPhotoCommentsCount(photoId) => Future { service.photoCommentsCount(photoId) } pipeTo sender

    case GetPhotoComments(locationPhotoId, offsetLimit) => Future { service.photoComments(locationPhotoId, offsetLimit) } pipeTo sender

  }

}
