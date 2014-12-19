package com.tooe.core.usecase.location_photo

import akka.actor.Actor
import akka.pattern._
import com.tooe.api.service.AddLocationPhotoRequest
import com.tooe.api.service.AddLocationPhotoUrlRequest
import com.tooe.api.service.ChangeLocationPhotoRequest
import com.tooe.api.service.DigitalSign
import com.tooe.api.service.PhotoMessage
import com.tooe.api.service._
import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.db.mongo.domain.LocationPhoto
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.db.mongo.domain.LocationPhotoComment
import com.tooe.core.db.mongo.domain.LocationPhotoLike
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.LocationPhotoCommentId
import com.tooe.core.domain.LocationPhotoId
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.domain.UserId
import com.tooe.core.domain._
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.usecase.ImageInfo
import com.tooe.core.usecase._
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.usecase.location.LocationDataActor.UpdateLocationPhotos
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.AddUserComment
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.FindLocationPhoto
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.GetLastLocationPhotos
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.RemoveLocationPhoto
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.SaveLocationPhoto
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.UpdateUserLikesAfterUnlike
import com.tooe.core.usecase.location_photo_comment.LocationPhotoCommentDataActor
import com.tooe.core.usecase.location_photo_like.LocationPhotoLikeDataActor
import com.tooe.core.usecase.location_photoalbum.{LocationPhotoAlbumWriteActor, LocationPhotoAlbumDataActor}
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor.UpdatePhotosCount
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import com.tooe.core.util.{Lang, ActorHelper}
import scala.Some
import scala.concurrent.Future

object LocationPhotoWriteActor {
  final val Id = Actors.LocationPhotoWrite

  case class CreatePhotoFromUrl(request: AddLocationPhotoUrlRequest)
  case class CreatePhotoFromValue(request: AddLocationPhotoRequest)
  case class LikeLocationPhoto(locationPhotoId: LocationPhotoId, userId: UserId)
  case class UnlikeLocationPhoto(locationPhotoId: LocationPhotoId, userId: UserId)
  case class AddLocationPhotoComment(locationPhotoId: LocationPhotoId, userId: UserId, comment: PhotoMessage, dsign: DigitalSign, lang: Lang)
  case class DeleteLocationPhoto(locationPhotoId: LocationPhotoId)
  case class UpdateLocationPhotosAfterDelete(locationId: LocationId)
  case class ChangeLocationPhoto(locationPhotoId: LocationPhotoId, request: ChangeLocationPhotoRequest)
}

class LocationPhotoWriteActor extends Actor with ActorHelper with AppActors with MediaServerTimeout {

  import LocationPhotoWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val locationPhotoAlbumDataActor = lookup(LocationPhotoAlbumDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val uploadMediaServerActor = lookup(UploadMediaServerActor.Id)
  lazy val locationPhotoLikeDataActor = lookup(LocationPhotoLikeDataActor.Id)
  lazy val locationPhotoCommentDataActor = lookup(LocationPhotoCommentDataActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val cacheWriteSnifferActor = lookup(CacheWriteSnifferActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val locationPhotoAlbumWriteActor = lookup(LocationPhotoAlbumWriteActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  def receive = {

    case CreatePhotoFromValue(request) =>
      (locationPhotoAlbumDataActor ?  LocationPhotoAlbumDataActor.FindLocationPhotoAlbum(request.locationAlbumId)).mapTo[LocationPhotoAlbum].flatMap { photoAlbum =>
        (uploadMediaServerActor ? UploadMediaServerActor.SavePhoto(ImageInfo(request.value, ImageType.locationPhoto, photoAlbum.locationId.id))).mapTo[String].flatMap { photoUrl =>
          (self ? CreatePhotoFromUrl(AddLocationPhotoUrlRequest(photoUrl, request.name, request.locationAlbumId, Some(photoAlbum)))).mapTo[SuccessfulResponse]
        }
      } pipeTo sender

    case CreatePhotoFromUrl(request) =>
      val albumFuture = request.photoAlbum.map { album =>
        Future successful album
      } getOrElse {
        (locationPhotoAlbumDataActor ? LocationPhotoAlbumDataActor.FindLocationPhotoAlbum(request.locationAlbumId)).mapTo[LocationPhotoAlbum]
      }
      val result = for {
        album <- albumFuture
        photo <- (locationPhotoDataActor ? SaveLocationPhoto(LocationPhoto(photoAlbumId = album.id, locationId = album.locationId, name = request.name, fileUrl = MediaObject(MediaObjectId(request.url))))).mapTo[LocationPhoto]
      } yield {
        locationDataActor ! LocationDataActor.AddPhotoToLocation(album.locationId, photo.id)
        locationPhotoAlbumDataActor ! UpdatePhotosCount(album.id, 1)
        urlsWriteActor ! UrlsWriteActor.AddLocationPhoto(photo.id, photo.fileUrl.url)
        LocationPhotoCreated(LocationPhotoIdCreated(photo.id))
      }
      result pipeTo sender

    case LikeLocationPhoto(locationPhotoId, userId) =>
      locationPhotoLikeDataActor ! LocationPhotoLikeDataActor.LikePhoto(LocationPhotoLike(locationPhotoId = locationPhotoId, userId = userId))
      sender ! SuccessfulResponse

    case UnlikeLocationPhoto(locationPhotoId, userId) =>
      val result = for {
        _ <- locationPhotoLikeDataActor ? LocationPhotoLikeDataActor.DeleteLike(locationPhotoId, userId)
        lastLikes <- (locationPhotoLikeDataActor ? LocationPhotoLikeDataActor.GetLastLikes(locationPhotoId)).mapTo[List[LocationPhotoLike]]
      } yield {
        locationPhotoDataActor ! UpdateUserLikesAfterUnlike(locationPhotoId, lastLikes map (_.userId))
      }
     sender ! SuccessfulResponse

    case AddLocationPhotoComment(locationPhotoId, userId, comment, dsign, lang) =>
      cacheWriteSnifferActor.ask(CacheWriteSnifferActor.IsWriteActionAllowed(userId, dsign, lang)).mapTo[Boolean].flatMap(_ => {
      val photoComment = LocationPhotoComment(locationPhotoId = locationPhotoId, userId = userId, message = comment.message)
      (locationPhotoCommentDataActor ? LocationPhotoCommentDataActor.AddComment(photoComment)).mapTo[LocationPhotoComment].map { comment =>
        locationPhotoDataActor ! AddUserComment(locationPhotoId, userId)
        LocationPhotoCommentCreatedResponse(comment)
      }}) pipeTo sender

    case DeleteLocationPhoto(photoId) =>
      val result = for {
        photo <- (locationPhotoDataActor ? FindLocationPhoto(photoId)).mapTo[LocationPhoto]
        _ <- locationPhotoDataActor ? RemoveLocationPhoto(photoId)
        _ <- locationPhotoAlbumDataActor ? UpdatePhotosCount(photo.photoAlbumId, -1)
      } yield {
        self ! UpdateLocationPhotosAfterDelete(photo.locationId)
        deleteMediaServerActor ! DeletePhotoFile(Seq(ImageInfo(photo.fileUrl.url.id, ImageType.locationPhoto, photo.locationId.id)))
        locationPhotoAlbumWriteActor ! LocationPhotoAlbumWriteActor.DeleteLocationsPhotoAlbumIfEmpty(photo.photoAlbumId)
        urlsDataActor ! UrlsDataActor.DeleteUrlByEntityAndUrl(photo.id.id -> photo.fileUrl.url)
        SuccessfulResponse
      }
      result pipeTo sender

    case UpdateLocationPhotosAfterDelete(locationId) =>
      (locationPhotoDataActor ? GetLastLocationPhotos(locationId)).mapTo[Seq[LocationPhoto]].map { photos =>
        locationDataActor ! UpdateLocationPhotos(locationId, photos.take(6) map (_.id))
      }

    case ChangeLocationPhoto(locationPhotoId, request) =>
      locationPhotoDataActor ! LocationPhotoDataActor.ChangeLocationPhoto(locationPhotoId, request)
      sender ! SuccessfulResponse

  }

}

case class LocationPhotoCommentCreatedResponse(comment: LocationPhotoCommentIdCreated) extends SuccessfulResponse

case class LocationPhotoCommentIdCreated(id: LocationPhotoCommentId)

object LocationPhotoCommentCreatedResponse {

  def apply(comment: LocationPhotoComment): LocationPhotoCommentCreatedResponse = {
    LocationPhotoCommentCreatedResponse(
      LocationPhotoCommentIdCreated(comment.id)
    )
  }

}