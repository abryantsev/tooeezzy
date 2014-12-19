package com.tooe.core.usecase

import akka.actor.Actor
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PhotoAlbumDataService
import com.tooe.core.domain.{PhotoAlbumId, UserId}
import com.tooe.api.service.OffsetLimit
import akka.pattern.pipe
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.PhotoAlbum
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.usecase.PhotoAlbumWriteActor.EditPhotoAlbumFields
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object PhotoAlbumDataActor {

  final val Id = Actors.PhotoAlbumData

  case class CreatePhotoAlbum(photoAlbum: PhotoAlbum)

  case class GetPhotoAlbumById(albumId: PhotoAlbumId)

  case class GetPhotoAlbumsByIds(albumIds: Set[PhotoAlbumId])

  case class GetPhotoAlbumsCountByUserId(userId: UserId)

  case class GetPhotoAlbumsByUserId(userId: UserId, offsetLimit: OffsetLimit)

  case class DeletePhotoAlbumById(albumId: PhotoAlbumId)

  case class UpdatePhotoAlbum(albumId: PhotoAlbumId, editFields: EditPhotoAlbumFields)

  case class ChangePhotoCounter(albumId: PhotoAlbumId, count: Int)

  case class GetUserDefaultPhotoAlbumId(userId: UserId)

}

class PhotoAlbumDataActor extends Actor with AppActors with DefaultTimeout {

  import PhotoAlbumDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PhotoAlbumDataService]

  def receive = {

    case GetPhotoAlbumsCountByUserId(userId) => Future {
      service.photoAlbumsCountByUser(userId)
    } pipeTo sender

    case GetPhotoAlbumsByUserId(userId, offsetLimit) =>
      Future {
        service.findPhotoAlbumsByUser(userId, offsetLimit)
      } pipeTo sender
    case GetPhotoAlbumsByIds(albumIds) =>
      Future { service.findPhotoAlbumsByIds(albumIds)} pipeTo sender

    case CreatePhotoAlbum(photoAlbum) =>
      Future {
        val entity = service.save(photoAlbum)
        entity.id
      } pipeTo sender

    case DeletePhotoAlbumById(albumId) => Future {
      service.delete(albumId)
    }

    case UpdatePhotoAlbum(albumId, editFields) => Future {
      service.update(albumId, editFields)
    }

    case GetPhotoAlbumById(albumId) =>
      Future {
        service.findOne(albumId).getOrNotFound(albumId.id, "photoalbum")
      } pipeTo sender

    case ChangePhotoCounter(albumId, count) =>
      Future {
        service.changePhotoCount(albumId, count)
      } pipeTo sender

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(PhotoAlbumId(msg.url.entityId), msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(PhotoAlbumId(msg.url.entityId)) }

    case GetUserDefaultPhotoAlbumId(userId) => Future { service.getUserDefaultPhotoAlbumId(userId) } pipeTo(sender)

  }

}