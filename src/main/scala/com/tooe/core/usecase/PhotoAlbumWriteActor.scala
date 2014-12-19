package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.service._
import scala.concurrent.Future
import akka.pattern._
import com.tooe.core.domain._
import akka.actor.Actor
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.PhotoAlbumDataActor._
import com.tooe.core.usecase.InfoMessageActor.GetMessage
import com.tooe.core.usecase.PhotoDataActor._
import com.tooe.core.db.mongo.domain.PhotoAlbum
import scala.Some
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.usecase.UploadMediaServerActor.SavePhoto
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.usecase.PhotoDataActor.DeletePhotosFromAlbum
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.main.SharedActorSystem
import com.tooe.extensions.scala.Settings

object PhotoAlbumWriteActor {
  final val Id = Actors.PhotoAlbumWrite

  case class AddPhotoAlbum(userId: UserId, name: Option[String], description: Option[String], usergroups: Option[UserGroups], mainphoto: AddPhotoParams, ctx: RouteContext)

  case class DeletePhotoAlbum(id: PhotoAlbumId, userId: UserId)

  case class EditPhotoAlbum(request: EditPhotoAlbumRequest, userId: UserId, id: PhotoAlbumId)

  case class EditPhotoAlbumFields(name: Option[String], usergroups: Option[UserGroups], description: Option[String], photoUrl: Option[MediaObject])

  case class CreatePhotoAlbumIfNotExist(photoAlbumId: Option[PhotoAlbumId], userId: UserId, photoUrl: String, context: RouteContext)

  case class DeletePhotoAlbumIfEmpty(albumId: PhotoAlbumId, userId: UserId)
}

class PhotoAlbumWriteActor extends Actor with AppActors with MediaServerTimeout {

  import PhotoAlbumWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val uploadServerActor = lookup(UploadMediaServerActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)
  lazy val photoWriteActor = lookup(PhotoWriteActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  lazy val settings = Settings(SharedActorSystem.sharedMainActorSystem)

  def receive = {

    case request: AddPhotoAlbum =>
      createPhotoAlbum(request) map { case (photoAlbumId, mainPhotoId) =>
        updateStatisticActor ! UpdateStatisticActor.ChangeUsersPhotoAlbumsCounter(request.userId, 1)
        userDataActor ! UserDataActor.AddPhotoAlbum(request.userId, photoAlbumId)
        newsWriteActor ! NewsWriteActor.AddPhotoAlbumNews(request.userId, photoAlbumId)
        PhotoAlbumCreated(PhotoAlbumCreatedId(photoAlbumId, mainPhotoId))
      } pipeTo sender

    case CreatePhotoAlbumIfNotExist(photoAlbumId, userId, photoUrl, ctx) =>
      photoAlbumId.map {
        albumId =>
          sender ! albumId
      } getOrElse {
        (infoMessageActor ? GetMessage("photoalbum_default_name", ctx.langId)).mapTo[String].flatMap {
          albumName =>
            photoAlbumDataActor ? CreatePhotoAlbum(PhotoAlbum(userId = userId, frontPhotoUrl = MediaObject(MediaObjectId(photoUrl)), name = albumName, default = Some(true)))

        } pipeTo sender
      }

    case DeletePhotoAlbum(albumId, userId) =>
      getAllPhotosByAlbum(albumId).zip(getAlbumFtr(albumId)).map { case (photos: Seq[Photo], album: PhotoAlbum) =>
          (photoDataActor ? DeletePhotosFromAlbum(albumId)).map {
            _ => photoWriteActor ! PhotoWriteActor.UpdateUserPhotosAfterDelete(userId)
          }
          deleteMediaServerActor ! DeletePhotoFile(photos map (p => ImageInfo(p.fileUrl.url.id, ImageType.photo, p.id.id)))
          urlsDataActor ! UrlsDataActor.DeleteUrlsByEntityAndUrl((album.id.id -> album.frontPhotoUrl.url) :: photos.map(p => (p.id.id, p.fileUrl.url)))
          deletePhotoAlbumAndUpdateStatistics(albumId, userId)
          SuccessfulResponse
      } pipeTo sender

    case EditPhotoAlbum(updateRequest, userId, id) =>
      def photoByIdFtr(id: PhotoId) = photoDataActor.ask(GetPhotoById(id)).mapTo[Photo] map (v => Some(v))
      updateRequest.photoid.map(id => photoByIdFtr(id)).getOrElse(Future successful None).map {
        photo =>
          photoAlbumDataActor ! PhotoAlbumDataActor.UpdatePhotoAlbum(id, EditPhotoAlbumFields(updateRequest.name, updateRequest.usergroups, updateRequest.description, photo.map(_.fileUrl)))
          photo.map { photo =>
            photo.fileUrl.mediaType.map { mo =>
              urlsWriteActor ! UrlsWriteActor.AddPhotoAlbumFrontPhoto(id, photo.fileUrl.url)
            }
          }
          SuccessfulResponse
      } pipeTo sender

    case DeletePhotoAlbumIfEmpty(albumId, userId) =>
      getAlbumFtr(albumId).map { album =>
        if(album.count == 0)
          deletePhotoAlbumAndUpdateStatistics(albumId, userId)
      }

  }


  def deletePhotoAlbumAndUpdateStatistics(albumId: PhotoAlbumId, userId: UserId) {
    photoAlbumDataActor ! DeletePhotoAlbumById(albumId)
    updateStatisticActor ! UpdateStatisticActor.ChangeUsersPhotoAlbumsCounter(userId, -1)
    userDataActor ! UserDataActor.RemovePhotoAlbum(userId, albumId)
  }

  def getAllPhotosByAlbum(albumId: PhotoAlbumId): Future[List[Photo]] = {
    (photoDataActor ? GetAllPhotosByAlbum(albumId)).mapTo[List[Photo]]
  }

  def createPhotoAlbum(request: AddPhotoAlbum): Future[(PhotoAlbumId, PhotoId)] = {
    import request._
    for {
      (photoUrl, albumName) <- getPhotoUrlFtr(mainphoto, userId) zip getAlbumNameFtr(name, ctx)
      photoAlbum = PhotoAlbum(
        name = albumName,
        description = description,
        userId = userId,
        count = 1,
        frontPhotoUrl = MediaObject(MediaObjectId(photoUrl)),
        allowedView = usergroups.flatMap(_.view).getOrElse(Nil),
        allowedComment = usergroups.flatMap(_.comments).getOrElse(Nil)
      )
      photoAlbumId <- (photoAlbumDataActor ? CreatePhotoAlbum(photoAlbum)).mapTo[PhotoAlbumId]
    } yield {
      val photoUrlId = MediaObjectId(photoUrl)
      val frontPhoto = Photo(
        fileUrl = MediaObject(photoUrlId),
        name = mainphoto.name,
        photoAlbumId = photoAlbumId,
        userId = userId
      )
      photoWriteActor ! PhotoWriteActor.CreatePhoto(frontPhoto, userId)
      urlsWriteActor ! UrlsWriteActor.AddPhotoAlbumFrontPhoto(photoAlbum.id, photoUrlId)
      (photoAlbumId, frontPhoto.id)
    }
  }

  def getAlbumFtr(albumId: PhotoAlbumId): Future[PhotoAlbum] = (photoAlbumDataActor ? PhotoAlbumDataActor.GetPhotoAlbumById(albumId)).mapTo[PhotoAlbum]

  def getAlbumNameFtr(name: Option[String], ctx: RouteContext): Future[String] =
    name.map(albumName => Future successful albumName)
      .getOrElse(infoMessageActor ? GetMessage("photoalbum_new_name", ctx.langId)).mapTo[String]

  def getPhotoUrlFtr(mainphoto: AddPhotoParams, userId: UserId): Future[String] =
    mainphoto.photoFormat.map { _ =>
      (uploadServerActor ? SavePhoto(ImageInfo(mainphoto.filePath, ImageType.photo, userId.id))).mapTo[String]
    } getOrElse {
      Future.successful(mainphoto.filePath)
    }

}

case class PhotoAlbumCreated(photoalbum: PhotoAlbumCreatedId) extends SuccessfulResponse

case class PhotoAlbumCreatedId(id: PhotoAlbumId, @JsonProperty("mainphotoid") mainPhoto: PhotoId)