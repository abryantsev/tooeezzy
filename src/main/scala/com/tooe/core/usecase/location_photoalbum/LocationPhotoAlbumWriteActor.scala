package com.tooe.core.usecase.location_photoalbum

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.service._
import scala.concurrent.Future
import com.tooe.core.usecase._
import com.tooe.core.usecase.location_photo.{LocationPhotoWriteActor, LocationPhotoDataActor}
import com.tooe.core.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor._
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor._
import com.tooe.core.db.mongo.domain._
import com.tooe.api.service.ChangeLocationPhotoAlbumRequest
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import akka.actor.Actor
import com.tooe.core.util.ActorHelper
import akka.pattern.{AskSupport, PipeToSupport}

object LocationPhotoAlbumWriteActor {
  final val Id = Actors.LocationPhotoAlbumWrite

  case class CreateLocationPhotoAlbum(request: AddLocationPhotoAlbumRequest, routeContext: RouteContext)
  case class DeleteLocationsPhotoAlbum(locationPhotoAlbumId: LocationPhotoAlbumId, locationPhotoAlbum: Option[LocationPhotoAlbum] = None)
  case class ChangeLocationPhotoAlbum(locationPhotoAlbumId: LocationPhotoAlbumId, request: ChangeLocationPhotoAlbumRequest)
  case class DeleteLocationsPhotoAlbumIfEmpty(locationPhotoAlbumId: LocationPhotoAlbumId)
}

class LocationPhotoAlbumWriteActor extends Actor with ActorHelper with AppActors with PipeToSupport with AskSupport with MediaServerTimeout with ExecutionContextProvider {

  import LocationPhotoAlbumWriteActor._

  lazy val infoMessageActor = lookup(InfoMessageActor.Id)
  lazy val uploadServerActor = lookup(UploadMediaServerActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val locationPhotoAlbumDataActor = lookup(LocationPhotoAlbumDataActor.Id)
  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val locationPhotoWriteActor = lookup(LocationPhotoWriteActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  def receive = {

    case CreateLocationPhotoAlbum(request, ctx) =>
      val result = for {
        location <- getActiveOrDeactivatedLocation(request.locationId)
        album = LocationPhotoAlbum(
          name = request.name,
          description = request.description,
          photosCount = 1,
          frontPhotoUrl = MediaObject(request.mainPhoto.url),
          locationId = request.locationId,
          locationsChainId = location.locationsChainId
        )
        albumId <- addLocationPhotoAlbum(album)
        photo <- saveLocationPhoto(request.mainPhoto, request.locationId, albumId)
      } yield {
        updateStatisticActor ! UpdateStatisticActor.ChangeLocationPhotoAlbumsCounter(request.locationId, 1)
        locationDataActor ! LocationDataActor.AddPhotoAlbumToLocation(location.id, albumId)
        urlsWriteActor ! UrlsWriteActor.AddLocationPhotoAlbum(album.id, album.frontPhotoUrl.url)
        LocationPhotoAlbumCreated(LocationPhotoAlbumIdCreated(albumId, photo.id))
      }
      result pipeTo sender


    case DeleteLocationsPhotoAlbum(albumId, album) =>
      val getPhotosAndAlbumFuture = (locationPhotoDataActor ? GetAllLocationPhotosByAlbum(albumId))
        .zip(album.map(Future.successful).getOrElse(findLocationPhotoAlbumFtr(albumId)))
      getPhotosAndAlbumFuture.mapTo[(List[LocationPhoto], LocationPhotoAlbum)].map { case (photos: List[LocationPhoto], album: LocationPhotoAlbum) =>
        updateStatisticActor ! UpdateStatisticActor.ChangeLocationPhotoAlbumsCounter(album.locationId, -1)
        locationPhotoAlbumDataActor ! DeleteLocationPhotoAlbum(albumId)
        locationDataActor ! LocationDataActor.DeletePhotoAlbumFromLocation(album.locationId, album.id)
        (locationPhotoDataActor ? DeleteLocationPhotoByAlbum(albumId)).map { _ => locationPhotoWriteActor ! LocationPhotoWriteActor.UpdateLocationPhotosAfterDelete(album.locationId) }
        deleteMediaServerActor ! DeletePhotoFile(photos map (p => ImageInfo(p.fileUrl.url.id, ImageType.locationPhoto, p.locationId.id)))
        urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl((album.id.id -> album.frontPhotoUrl.url) :: photos.map(p => p.id.id -> p.fileUrl.url))
        SuccessfulResponse
      } pipeTo sender

    case DeleteLocationsPhotoAlbumIfEmpty(albumId) => findLocationPhotoAlbumFtr(albumId) map { album =>
       if(album.photosCount == 0)
         self ! DeleteLocationsPhotoAlbum(albumId, Some(album))
    }


    case ChangeLocationPhotoAlbum(locationPhotoAlbumId, request) =>
      val photoFtr = request.photoId.map { photoId =>
        (locationPhotoDataActor ? GetLocationPhoto(photoId)).mapTo[Option[LocationPhoto]]
      } getOrElse Future.successful(None)

      photoFtr.map { photo =>
        locationPhotoAlbumDataActor ! UpdateLocationPhotoAlbum(locationPhotoAlbumId, UpdateLocationPhotoAlbumRequest(request.name, request.description, photo.map(_.fileUrl)))
        photo.map { photo =>
          photo.fileUrl.mediaType.map { _ =>
            urlsWriteActor ! UrlsWriteActor.AddLocationPhotoAlbum(locationPhotoAlbumId, photo.fileUrl.url)
          }
        }
        SuccessfulResponse
      } pipeTo sender

  }

  def findLocationPhotoAlbumFtr(albumId: LocationPhotoAlbumId): Future[LocationPhotoAlbum] =
    (locationPhotoAlbumDataActor ? FindLocationPhotoAlbum(albumId)).mapTo[LocationPhotoAlbum]

  def saveLocationPhoto(photo: LocationMainPhoto, locationId: LocationId, albumId: LocationPhotoAlbumId): Future[LocationPhoto] = {
    (locationPhotoDataActor ? SaveLocationPhoto(LocationPhoto(fileUrl = MediaObject(photo.url), name = photo.name, locationId = locationId, photoAlbumId = albumId))).mapTo[LocationPhoto]
  }

  def addLocationPhotoAlbum(album: LocationPhotoAlbum): Future[LocationPhotoAlbumId] = {
    (locationPhotoAlbumDataActor ? AddLocationPhotoAlbum(album)).mapTo[LocationPhotoAlbumId]
  }

  def getLocation(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocation(id)).mapTo[Location]
  def getActiveOrDeactivatedLocation(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetActiveLocationOrByLifeCycleStatuses(id, Seq(LifecycleStatusId.Deactivated))).mapTo[Location]
}

case class LocationPhotoAlbumCreated(@JsonProperty("locationalbum") locationPhotoAlbumId: LocationPhotoAlbumIdCreated) extends SuccessfulResponse

case class LocationPhotoAlbumIdCreated(@JsonProperty("id") albumId: LocationPhotoAlbumId,
                                       @JsonProperty("mainphotoid") photoId: LocationPhotoId)