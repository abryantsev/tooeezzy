package com.tooe.core.usecase

import akka.actor.Actor
import akka.pattern.pipe
import akka.pattern.ask
import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.service._
import scala.Predef._
import com.tooe.core.domain._
import scala.concurrent.Future
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor
import com.tooe.core.usecase.PhotoAlbumDataActor.GetPhotoAlbumById
import com.tooe.core.db.mongo.domain.PhotoLike
import com.tooe.core.usecase.PhotoAlbumDataActor.GetPhotoAlbumsByUserId
import com.tooe.core.db.mongo.domain.PhotoAlbum
import com.tooe.core.usecase.PhotoDataActor.GetPhotoByAlbum
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.usecase.PhotoAlbumDataActor.GetPhotoAlbumsCountByUserId
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.util.Images
import com.tooe.core.util.MediaHelper._

object PhotoAlbumReadActor {
  final val Id = Actors.PhotoAlbumRead

  case class GetPhotoAlbum(albumId: PhotoAlbumId, offsetLimit: OffsetLimit, userId: UserId, viewType: ViewType)

  case class GetPhotoAlbumsByUser(userId: UserId, offsetLimit: OffsetLimit, currentUserId: UserId)

  case class GetPhotosByAlbum(albumId: PhotoAlbumId, offsetLimit: OffsetLimit, userId: UserId)
}

class PhotoAlbumReadActor extends Actor with AppActors with MediaServerTimeout {

  import PhotoAlbumReadActor._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val photoLikeDataActor = lookup(PhotoLikeDataActor.Id)

  def receive = {

    case GetPhotoAlbum(albumId, offsetLimit, userId, viewType) =>
      val result = for {
        photoAlbum <- (photoAlbumDataActor ? GetPhotoAlbumById(albumId)).mapTo[PhotoAlbum]
        photos <- getAlbumPhotos(albumId, offsetLimit, viewType)
      } yield {
        viewType match {
          case ViewType.Short => PhotoAlbumShortResponse(PhotoAlbumShortItem(photoAlbum))
          case _ => PhotoAlbumResponse(PhotoAlbumItem(photoAlbum, photos))
        }

      }
      result pipeTo sender

    case GetPhotoAlbumsByUser(userId, offsetLimit, currentUserId) =>
      val result = for {
        photoAlbumsCount <- photoAlbumDataActor.ask(GetPhotoAlbumsCountByUserId(userId)).mapTo[Long]
        photoAlbumPage <- photoAlbumDataActor.ask(GetPhotoAlbumsByUserId(userId, offsetLimit)).mapTo[List[PhotoAlbum]]
      } yield PhotoAlbumsResponse(photoAlbumsCount, photoAlbumPage map PhotoAlbumInfo.apply)
      result pipeTo sender

    case GetPhotosByAlbum(albumId, offsetLimit, userId) =>
      (for {
        photos <- (photoDataActor ? PhotoDataActor.GetPhotoByAlbum(albumId, offsetLimit)).mapTo[Seq[Photo]]
        likes <- (photoLikeDataActor ? PhotoLikeDataActor.UserPhotosLikes(userId, photos.map(_.id))).mapTo[Seq[PhotoLike]]
      } yield {
        PhotosResponse(photos, likes)
      }) pipeTo sender

  }


  def getAlbumPhotos(albumId: PhotoAlbumId, offsetLimit: OffsetLimit, viewType: ViewType): Future[List[Photo]] = {
    if(viewType == ViewType.Short)
      Future successful Nil
    else
      (photoDataActor ? GetPhotoByAlbum(albumId, offsetLimit)).mapTo[List[Photo]]
  }

  def photoAlbumsToImageType(photoAlbums: Seq[PhotoAlbum]): Seq[ImageInfo] =
    photoAlbums map (photoAlbum => ImageInfo(photoAlbum.frontPhotoUrl.url.id, ImageType.photo, photoAlbum.userId.id))

}

case class PhotoAlbumResponse(photoalbum: PhotoAlbumItem) extends SuccessfulResponse

case class PhotoAlbumItem
(
  id: PhotoAlbumId,
  name: String,
  description: Option[String],
  photoscount: Long,
  photos: Seq[PhotoItem],
  usergroups: UserGroupsItem
  )

object PhotoAlbumItem {
  def apply(photoAlbum: PhotoAlbum, photos: Seq[Photo]): PhotoAlbumItem =
    PhotoAlbumItem(
      id = photoAlbum.id,
      name = photoAlbum.name,
      description = photoAlbum.description,
      photoscount = photoAlbum.count,
      photos = photos map PhotoItem.apply,
      usergroups = UserGroupsItem(photoAlbum)
    )
}

case class PhotoItem(id: PhotoId, name: String, media: MediaUrl)

object PhotoItem {
  def apply(photo: Photo): PhotoItem =
    PhotoItem(
      id = photo.id,
      name = photo.name.getOrElse(""),
      photo.fileUrl.asMediaUrl(Images.Photoalbum.Full.Self.Media)
    )
}

case class PhotoAlbumsResponse
(
  photoalbumscount: Long,
  photoalbums: Seq[PhotoAlbumInfo]
  ) extends SuccessfulResponse

case class PhotoAlbumInfo
(
  id: PhotoAlbumId,
  name: String,
  description: Option[String],
  photoscount: Long,
  media: MediaUrl
  )

object PhotoAlbumInfo {
  def apply(album: PhotoAlbum): PhotoAlbumInfo =
    PhotoAlbumInfo(
      id = album.id,
      name = album.name,
      description = album.description,
      photoscount = album.count,
      media = album.frontPhotoUrl.asMediaUrl(Images.Userphotoalbums.Full.Self.Media)
    )
}

case class UserGroupsItem(view: Seq[String], comments: Seq[String])

object UserGroupsItem {

  def apply(album: PhotoAlbum): UserGroupsItem = UserGroupsItem(album.allowedView, album.allowedComment)

}

case class PhotoAlbumShortResponse(photoalbum: PhotoAlbumShortItem) extends SuccessfulResponse

case class PhotoAlbumShortItem
(
  id: PhotoAlbumId,
  name: String,
  description: Option[String],
  photoscount: Long,
  usergroups: UserGroupsItem,
  media: MediaUrl
  )

object PhotoAlbumShortItem {
  def apply(photoAlbum: PhotoAlbum): PhotoAlbumShortItem =
    PhotoAlbumShortItem(
      id = photoAlbum.id,
      name = photoAlbum.name,
      description = photoAlbum.description,
      photoscount = photoAlbum.count,
      usergroups = UserGroupsItem(photoAlbum),
      media = photoAlbum.frontPhotoUrl.asMediaUrl(Images.Photoalbum.Short.Self.Media)
    )
}

case class PhotosResponse(photos: Seq[PhotoResponseItem]) extends SuccessfulResponse

object PhotosResponse {

  def apply(photos: Seq[Photo], userLikes: Seq[PhotoLike]): PhotosResponse = {
    PhotosResponse(
      photos.map { p =>
        PhotoResponseItem(p, userLikes.find(l => l.photoId == p.id).map(_ => true))
      }
    )
  }

}

case class PhotoResponseItem(id: PhotoId,
                             name: String,
                             media: MediaUrl,
                             @JsonProperty("commentscount") commentsCount: Int,
                             @JsonProperty("likescount") likesCount: Int,
                             @JsonProperty("selfliked") selfLiked: Option[Boolean])

object PhotoResponseItem {

  def apply(photo: Photo, liked: Option[Boolean]): PhotoResponseItem =
    PhotoResponseItem(
      id = photo.id,
      name = photo.name.getOrElse(""),
      media = photo.fileUrl.asMediaUrl(Images.Photoalbumphotos.Full.Photo.Media),
      commentsCount = photo.commentsCount,
      likesCount = photo.likesCount,
      selfLiked = liked
    )

}