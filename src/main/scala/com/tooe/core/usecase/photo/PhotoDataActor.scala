package com.tooe.core.usecase

import akka.actor.Actor
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PhotoDataService
import com.tooe.core.domain.{PhotoId, PhotoAlbumId, UserId}
import com.tooe.api.service.{PhotoChangeRequest, OffsetLimit}
import akka.pattern.pipe
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object  PhotoDataActor {

  final val Id = Actors.PhotoData

  case class CreatePhoto(photo: Photo)
  case class DeletePhoto(photoId: PhotoId)
  case class DeletePhotosFromAlbum(albumId: PhotoAlbumId)
  case class FindPhoto(photoId: PhotoId)
  case class GetAllPhotosByAlbum(albumId: PhotoAlbumId)
  case class GetPhotoByAlbum(albumId: PhotoAlbumId, offsetLimit: OffsetLimit)
  case class GetCountPhotoByAlbum(albumId: PhotoAlbumId)
  case class GetPhotoAlbumId(photoId: PhotoId)
  case class GetPhotoById(photoId: PhotoId)
  case class GetPhotos(photoIds: Seq[PhotoId])
  case class UpdateUserLikes(photoId: PhotoId, userId: UserId)
  case class UpdateUserDislikes(photoId: PhotoId, userIds: List[UserId])
  case class AddUserComment(photoId: PhotoId, userId: UserId)
  case class DeleteUserComment(id: PhotoId, userIds: Seq[UserId])
  case class GetLastUserPhotos(userId: UserId)
  case class UpdatePhoto(photoId: PhotoId, request: PhotoChangeRequest)
}

class PhotoDataActor extends Actor with AppActors {

  import PhotoDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PhotoDataService]

  def receive = {

    case DeletePhotosFromAlbum(albumId) =>  Future { service.removePhotos(albumId) }

    case FindPhoto(photoId) =>
      Future { service.findOne(photoId) } pipeTo sender

    case GetPhotoByAlbum(albumId, offsetLimit) =>
      Future {
        service.findByAlbumId(albumId, offsetLimit)
      } pipeTo sender

    case GetAllPhotosByAlbum(albumId) =>
      Future {
        service.findByAlbumId(albumId)
      } pipeTo sender

    case GetCountPhotoByAlbum(albumId) =>
      Future {
        service.countPhotosInAlbum(albumId)
      } pipeTo sender

    case CreatePhoto(photo) =>
      Future { service.save(photo) } pipeTo sender

    case DeletePhoto(photoId) =>
      Future {
        service.delete(photoId)
      } pipeTo sender

    case GetPhotoAlbumId(photoId) =>
      Future { service.findOne(photoId).map(_.photoAlbumId).getOrNotFound(photoId, "Photo not found") } pipeTo sender

    case GetPhotoById(photoId) =>
      Future { service.findOne(photoId).getOrNotFound(photoId, "Photo not found") } pipeTo sender

    case GetPhotos(photoIds) =>
      Future { service.getPhotos(photoIds).toSeq} pipeTo sender

    case UpdateUserLikes(photoId, userId) =>
      Future { service.updateUserLikes(photoId, userId) }

    case UpdateUserDislikes(photoId, userIds) =>
      Future { service.updateUserDislikes(photoId, userIds) }

    case AddUserComment(photoId, userIds) => service.addUserComment(photoId, userIds)

    case DeleteUserComment(id, userIds) => Future { service.deleteUserComment(id, userIds) }

    case GetLastUserPhotos(userId) => Future { service.getLastUserPhotos(userId) } pipeTo sender

    case UpdatePhoto(id, request) => Future { service.updatePhoto(id, request) }

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(PhotoId(msg.url.entityId), msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(PhotoId(msg.url.entityId)) }

  }

}
