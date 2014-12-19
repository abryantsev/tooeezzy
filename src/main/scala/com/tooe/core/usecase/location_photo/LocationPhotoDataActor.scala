package com.tooe.core.usecase.location_photo

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.boot.DefaultTimeout
import akka.actor.Actor
import akka.pattern.pipe
import com.tooe.core.db.mongo.domain.LocationPhoto
import com.tooe.core.service.LocationPhotoDataService
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.domain.{LocationPhotoAlbumId, LocationId, UserId, LocationPhotoId}
import com.tooe.core.usecase._
import com.tooe.api.service.{ChangeLocationPhotoRequest, OffsetLimit}
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object LocationPhotoDataActor {

  final val Id = Actors.LocationPhotoData

  case class SaveLocationPhoto(photo: LocationPhoto)
  case class FindLocationPhoto(photoId: LocationPhotoId)
  case class GetLocationPhoto(photoId: LocationPhotoId)
  case class GetLocationPhotos(photoIds: Seq[LocationPhotoId])
  case class UpdateUserLikes(locationPhotoId: LocationPhotoId, userId: UserId)
  case class UpdateUserLikesAfterUnlike(locationPhotoId: LocationPhotoId, userIds: Seq[UserId])
  case class AddUserComment(locationPhotoId: LocationPhotoId, userId: UserId)
  case class RemoveLocationPhoto(photoId: LocationPhotoId)
  case class GetLastLocationPhotos(locationId: LocationId)
  case class GetAllLocationPhotosByAlbum(albumId: LocationPhotoAlbumId)
  case class DeleteLocationPhotoByAlbum(albumId: LocationPhotoAlbumId)
  case class CountByLocation(locationId: LocationId)
  case class GetLocationPhotoByAlbum(albumId: LocationPhotoAlbumId, offsetLimit: OffsetLimit)
  case class ChangeLocationPhoto(locationPhotoId: LocationPhotoId, request: ChangeLocationPhotoRequest)
}

class LocationPhotoDataActor extends Actor with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationPhotoDataActor._

  lazy val service = BeanLookup[LocationPhotoDataService]

  def receive = {

    case SaveLocationPhoto(photo) => Future { service.save(photo) } pipeTo sender
    case FindLocationPhoto(photoId) => Future { service.findOne(photoId).getOrNotFound(photoId, "Location photo not found") } pipeTo sender
    case GetLocationPhoto(photoId) => Future { service.findOne(photoId) } pipeTo sender
    case GetLocationPhotos(photoIds) => Future { service.getLocationPhotos(photoIds)} pipeTo sender
    case UpdateUserLikes(locationPhotoId, userId) => Future { service.updateUserLikes(locationPhotoId, userId) }
    case UpdateUserLikesAfterUnlike(locationPhotoId, userIds) => Future { service.updateUserLikes(locationPhotoId, userIds) }
    case AddUserComment(locationPhotoId, userId) => Future { service.addUserComment(locationPhotoId, userId) }
    case RemoveLocationPhoto(photoId) => Future { service.delete(photoId) } pipeTo sender
    case GetLastLocationPhotos(locationId) => Future { service.getLastLocationPhotos(locationId) } pipeTo sender
    case GetAllLocationPhotosByAlbum(albumId) => Future { service.getAllLocationPhotosByAlbum(albumId) } pipeTo sender
    case DeleteLocationPhotoByAlbum(albumId: LocationPhotoAlbumId) => Future { service.deletePhotosByAlbum(albumId) } pipeTo sender
    case CountByLocation(locationId) => Future { service.countByLocation(locationId) }
    case GetLocationPhotoByAlbum(albumId, offsetLimit) => Future { service.getLocationPhotos(albumId, offsetLimit) } pipeTo sender
    case ChangeLocationPhoto(locationPhotoId, request) => Future { service.changePhoto(locationPhotoId, request) }

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(LocationPhotoId(msg.url.entityId), msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(LocationPhotoId(msg.url.entityId)) }
  }

}
