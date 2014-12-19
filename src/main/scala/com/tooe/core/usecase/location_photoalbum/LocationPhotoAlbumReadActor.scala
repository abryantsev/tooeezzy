package com.tooe.core.usecase.location_photoalbum

import akka.actor.Actor
import akka.pattern.ask
import com.tooe.core.util.{Images, ActorHelper}
import com.tooe.core.util.MediaHelper._
import com.tooe.core.application.{Actors, AppActors}
import akka.pattern.pipe
import org.bson.types.ObjectId
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor._
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor.LocationPhotoAlbums
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.GetLocationPhotoByAlbum
import com.tooe.core.domain._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor.CountByLocation
import com.tooe.core.db.mongo.domain.{LocationPhotoLike, LocationPhotoAlbum, LocationPhoto}
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.api.boot.DefaultTimeout
import scala.concurrent.Future
import com.tooe.core.usecase.location_photo_like.LocationPhotoLikeDataActor
import com.tooe.core.usecase.location_photo_like.LocationPhotoLikeDataActor.GetLocationPhotoLikesByPhotos

object LocationPhotoAlbumReadActor {
  final val Id = Actors.LocationPhotoAlbumRead

  case class GetLocationsPhotoAlbumsByLocation(locationId: LocationId, offsetLimit: OffsetLimit)
  case class GetLocationsPhotoAlbum(albumId: LocationPhotoAlbumId, offsetLimit: OffsetLimit, viewType: ViewType)
  case class GetLocationsChainPhotoAlbums(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit)
  case class GetLocationsPhotoByAlbum(albumId: LocationPhotoAlbumId, offsetLimit: OffsetLimit)
}

class LocationPhotoAlbumReadActor extends Actor with ActorHelper with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationPhotoAlbumReadActor._

  lazy val locationPhotoAlbumDataActor = lookup(LocationPhotoAlbumDataActor.Id)
  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val locationPhotoLikeDataActor = lookup(LocationPhotoLikeDataActor.Id)

  def receive = {
    case GetLocationsPhotoAlbumsByLocation(locationId, pageRequest) =>
      val locationAlbumsCountFuture = locationPhotoAlbumDataActor ? CountByLocation(locationId)
      val locationAlbumsFuture = locationPhotoAlbumDataActor ? LocationPhotoAlbums(locationId, pageRequest)
      val result = for {
        (count, albums) <- (locationAlbumsCountFuture zip locationAlbumsFuture).mapTo[(Long, List[LocationPhotoAlbum])]
      } yield {
        GetLocationPhotoAlbumResponse(
          count,
          albums.map(LocationPhotoAlbumShortDetails.apply(_, Images.Lphotoalbumssearch.Full.Album.Media))
        )
      }
      result pipeTo sender

    case GetLocationsPhotoAlbum(albumId, offsetLimit, viewType) =>
      val result = for {
        photoAlbum <- (locationPhotoAlbumDataActor ? FindLocationPhotoAlbum(albumId)).mapTo[LocationPhotoAlbum]
        photos <- getPhotosWithUrl(albumId, offsetLimit, viewType)
      } yield {
        viewType match {
          case ViewType.None => LocationPhotoAlbumResponse(LocationPhotoAlbumDetails(photoAlbum, photos))
          case _ => LocationPhotoAlbumShortResponse(LocationPhotoAlbumShortItem(photoAlbum))
        }
      }
      result pipeTo sender

    case GetLocationsChainPhotoAlbums(locationsChainId, offsetLimit) =>
      (locationPhotoAlbumDataActor ? CountLocationsChainPhotoAlbums(locationsChainId))
        .zip(locationPhotoAlbumDataActor ? FindLocationsChainPhotoAlbums(locationsChainId, offsetLimit))
        .mapTo[(Long, Seq[LocationPhotoAlbum])].map { case (count, albums) =>
          GetLocationPhotoAlbumResponse(
            count,
            albums.map(LocationPhotoAlbumShortDetails.apply(_))
          )
      } pipeTo sender

    case GetLocationsPhotoByAlbum(albumId, offsetLimit) =>
      (for {
        photos <- (locationPhotoDataActor ? GetLocationPhotoByAlbum(albumId, offsetLimit)).mapTo[Seq[LocationPhoto]]
        likedPhotos <- (locationPhotoLikeDataActor ? GetLocationPhotoLikesByPhotos(photos.map(_.id).toSet)).mapTo[Seq[LocationPhotoLike]]
      } yield {
        LocationPhotosResponse(photos map (LocationPhotoItem(_, likedPhotos)))
      }) pipeTo sender
  }

  def getPhotosWithUrl(albumId: LocationPhotoAlbumId, offsetLimit: OffsetLimit, viewType: ViewType): Future[List[LocationPhoto]] = {
    viewType match {
      case ViewType.None =>
        (locationPhotoDataActor ? GetLocationPhotoByAlbum(albumId, offsetLimit)).mapTo[List[LocationPhoto]]
      case _ => Future successful List[LocationPhoto]()
    }
  }
}

case class GetLocationPhotoAlbumResponse
(
  @JsonProperty("photoalbumscount") photoAlbumCount: Long,
  @JsonProperty("photoalbums") photoAlbums: Seq[LocationPhotoAlbumShortDetails]
  ) extends SuccessfulResponse

case class LocationPhotoAlbumShortDetails
(
  id: ObjectId,
  name: String,
  @JsonProperty("photoscount") photosCount: Long,
  media: MediaUrl
  )

object LocationPhotoAlbumShortDetails {
  def apply(album: LocationPhotoAlbum, imageSize: String = Images.Lphotoalbumssearch.Full.Album.Media): LocationPhotoAlbumShortDetails =
    LocationPhotoAlbumShortDetails(
      id = album.id.id,
      name = album.name,
      photosCount = album.photosCount,
      media = album.frontPhotoUrl.asMediaUrl(imageSize)
    )
}

case class LocationPhotoAlbumResponse(@JsonProperty("photoalbum") photoAlbum: LocationPhotoAlbumDetails) extends SuccessfulResponse

case class LocationPhotoAlbumDetails
(
  id: LocationPhotoAlbumId,
  name: String,
  description: Option[String],
  @JsonProperty("photoscount") photosCount: Long,
  photos: Seq[LocationPhotoDetails]
  )

object LocationPhotoAlbumDetails {
  def apply(photoAlbum: LocationPhotoAlbum, photos: Seq[LocationPhoto]): LocationPhotoAlbumDetails =
    LocationPhotoAlbumDetails(
      id = photoAlbum.id,
      name = photoAlbum.name,
      description = photoAlbum.description,
      photosCount = photoAlbum.photosCount,
      photos = photos map LocationPhotoDetails.apply
    )
}

case class LocationPhotoAlbumShortResponse(@JsonProperty("photoalbum") photoAlbum: LocationPhotoAlbumShortItem) extends SuccessfulResponse

case class LocationPhotoAlbumShortItem
(
  id: LocationPhotoAlbumId,
  name: String,
  description: Option[String],
  @JsonProperty("photoscount") photosCount: Long,
  media: MediaUrl
  )

object LocationPhotoAlbumShortItem {
  def apply(photoAlbum: LocationPhotoAlbum): LocationPhotoAlbumShortItem =
    LocationPhotoAlbumShortItem(
      id = photoAlbum.id,
      name = photoAlbum.name,
      description = photoAlbum.description,
      photosCount = photoAlbum.photosCount,
      media = photoAlbum.frontPhotoUrl.asMediaUrl(Images.Lphotoalbum.Short.Self.Media)
    )
}

case class LocationPhotoDetails
(
  id: LocationPhotoId,
  name: String,
  media: MediaUrl)

object LocationPhotoDetails {
  def apply(photo: LocationPhoto): LocationPhotoDetails =
    LocationPhotoDetails(
      photo.id,
      photo.name getOrElse "",
      photo.fileUrl.asMediaUrl(Images.Lphotoalbum.Full.Self.Media)
    )
}

case class LocationPhotoItem(id: LocationPhotoId,
                             name: String,
                             media: MediaUrl,
                             @JsonProperty("commentscount") commentsCount: Int,
                             @JsonProperty("likescount") likesCount: Int,
                             @JsonProperty("selfliked") selfLiked: Option[Boolean])

object LocationPhotoItem {

  def apply(locationPhoto: LocationPhoto, likes: Seq[LocationPhotoLike]): LocationPhotoItem =
    LocationPhotoItem(
      id = locationPhoto.id,
      name = locationPhoto.name.getOrElse(""),
      media = locationPhoto.fileUrl.asMediaUrl(Images.Lphotoalbumphotos.Photos.Media),
      commentsCount = locationPhoto.commentsCount,
      likesCount = locationPhoto.likesCount,
      selfLiked = likes.find(_.locationPhotoId == locationPhoto.id).map(_ => true)
    )

}

case class LocationPhotosResponse(photos: Seq[LocationPhotoItem]) extends SuccessfulResponse