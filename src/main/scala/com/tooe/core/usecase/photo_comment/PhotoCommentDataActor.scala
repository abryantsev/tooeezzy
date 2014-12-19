package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PhotoCommentDataService
import akka.pattern.pipe
import scala.concurrent.Future
import com.tooe.core.domain.{PhotoCommentId, PhotoId}
import com.tooe.core.db.mongo.domain.PhotoComment
import com.tooe.api.service.OffsetLimit

object PhotoCommentDataActor {
  final val Id = Actors.PhotoCommentData

  case class AddComment(comment: PhotoComment)
  case class GetLastPhotoComments(photoId: PhotoId)
  case class DeleteComment(photoCommentId: PhotoCommentId)
  case class FindComment(photoCommentId: PhotoCommentId)
  case class GetComments(photoId: PhotoId, offsetLimit: OffsetLimit)
  case class GetCommentCount(photoId: PhotoId)

}

class PhotoCommentDataActor extends Actor with AppActors with DefaultTimeout  {

  import PhotoCommentDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PhotoCommentDataService]

  def receive = {

    case AddComment(comment) => Future { service.save(comment) } pipeTo sender

    case GetLastPhotoComments(photoId) => Future{ service.findPhotoComments(photoId, OffsetLimit(0, 20)) } pipeTo sender

    case FindComment(photoCommentId) =>
      Future { service.findOne(photoCommentId).getOrNotFound(photoCommentId, "PhotoComment not found") } pipeTo sender

    case DeleteComment(photoCommentId) =>
      Future { service.delete(photoCommentId) } pipeTo sender

    case GetComments(photoId, offsetLimit) =>
      Future { service.findPhotoComments(photoId, offsetLimit) } pipeTo sender

    case GetCommentCount(photoId) =>
      Future { service.commentsCount(photoId) } pipeTo sender

  }
}
