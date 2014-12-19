package com.tooe.core.usecase

import akka.pattern._
import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.service._
import akka.actor.Actor
import scala.concurrent.Future
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor
import com.tooe.core.usecase.user.UserDataActor
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.util.Lang
import com.tooe.core.usecase.urls.{UrlsDataActor, UrlsWriteActor}
import com.tooe.core.db.mongo.domain.PhotoLike
import com.tooe.core.usecase.PhotoCommentDataActor.GetLastPhotoComments
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor.DeleteLike
import com.tooe.api.service.PhotoChangeRequest
import com.tooe.core.usecase.PhotoCommentDataActor.DeleteComment
import com.tooe.core.usecase.PhotoCommentDataActor.FindComment
import com.tooe.core.domain.PhotoCommentId
import com.tooe.core.usecase.UploadMediaServerActor.SavePhoto
import com.tooe.api.service.RouteContext
import com.tooe.core.usecase.DeleteMediaServerActor.DeletePhotoFile
import com.tooe.core.usecase.PhotoDataActor._
import com.tooe.core.domain.PhotoId
import com.tooe.api.service.PhotoMessage
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor.GetLikes
import com.tooe.core.usecase.user.UserDataActor.AddPhotoToUser
import com.tooe.core.usecase.user.UserDataActor.UpdateUserPhotos
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor.Like
import com.tooe.core.db.mongo.domain.PhotoComment
import com.tooe.core.domain.UserId
import com.tooe.core.usecase.PhotoAlbumDataActor.ChangePhotoCounter
import com.tooe.core.usecase.PhotoDataActor.GetLastUserPhotos
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.usecase.PhotoCommentDataActor.AddComment
import com.tooe.core.usecase.PhotoDataActor.AddUserComment
import com.tooe.api.service.DigitalSign
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.usecase.PhotoDataActor.DeleteUserComment
import com.tooe.core.usecase.PhotoDataActor.UpdatePhoto
import com.tooe.extensions.scala.Settings
import com.tooe.core.main.SharedActorSystem
import com.tooe.core.exceptions.{ForbiddenAppException, ApplicationException}

object PhotoWriteActor {
  final val Id = Actors.PhotoWrite

  case class AddPhoto(parameters: AddPhotoParameters, userId: UserId, context: RouteContext)

  case class RemovePhoto(photoId: PhotoId, userId: UserId)

  case class ChangePhoto(photoId: PhotoId, request: PhotoChangeRequest, userId: UserId)

  case class LikePhoto(photoId: PhotoId, userId: UserId)

  case class DislikePhoto(photoId: PhotoId, userId: UserId)

  case class CommentPhoto(photoId: PhotoId, userId: UserId, comment: PhotoMessage, dsign: DigitalSign, lang: Lang)

  case class DeletePhotoComment(userId: UserId, commentId: PhotoCommentId)

  case class CreatePhoto(photo: Photo, userId: UserId)

  case class UpdateUserPhotosAfterDelete(userId: UserId)

}

class PhotoWriteActor extends Actor with AppActors with MediaServerTimeout {

  import PhotoWriteActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val uploadMediaServerActor = lookup(UploadMediaServerActor.Id)
  lazy val deleteMediaServerActor = lookup(DeleteMediaServerActor.Id)
  lazy val photoAlbumDataActor = lookup(PhotoAlbumDataActor.Id)
  lazy val photoAlbumWriteActor = lookup(PhotoAlbumWriteActor.Id)
  lazy val photoCommentsDataActor = lookup(PhotoCommentDataActor.Id)
  lazy val photoLikeDataActor = lookup(PhotoLikeDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val cacheWriteSnifferActor = lookup(CacheWriteSnifferActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val urlsWriteActor = lookup(UrlsWriteActor.Id)
  lazy val urlsDataActor = lookup(UrlsDataActor.Id)

  def receive = {

    case AddPhoto(parameters, userId, ctx) =>

      val photoUrlFuture = parameters.photoFormat.map {
        details =>
          uploadMediaServerActor.ask(SavePhoto(ImageInfo(parameters.file, ImageType.photo, userId.id))).mapTo[String]
      } getOrElse {
        Future.successful(parameters.file)
      }

      val result = for {
        photoUrl <- photoUrlFuture
        albumId <- getPhotoAlbumId(parameters.photoAlbumId, userId, photoUrl, ctx)
        photo = Photo(userId = userId, name = parameters.name, fileUrl = MediaObject(MediaObjectId(photoUrl)), photoAlbumId = albumId)
        savedPhoto <- (self ? CreatePhoto(photo, userId)).mapTo[Photo]
      } yield {
        photoAlbumDataActor ! ChangePhotoCounter(albumId, 1)
        newsWriteActor ! NewsWriteActor.AddPhotoNews(userId, albumId, photo.id)
        PhotoCreated(PhotoCreatedId(savedPhoto.id))
      }

      result pipeTo sender

    case CreatePhoto(photo, userId) =>
      (photoDataActor ? PhotoDataActor.CreatePhoto(photo)).mapTo[Photo].map {
        photo =>
          userDataActor ! AddPhotoToUser(userId, photo.id)
          urlsWriteActor ! UrlsWriteActor.AddPhoto(photo.id, photo.fileUrl.url)
          photo
      } pipeTo sender


    case RemovePhoto(photoId, userId) =>
      val result = for {
        photo <- (photoDataActor ? GetPhotoById(photoId)).mapTo[Photo]
        _ <- photoDataActor ? DeletePhoto(photoId)
        _ <- photoAlbumDataActor ? ChangePhotoCounter(photo.photoAlbumId, -1)
      } yield {
        self ! UpdateUserPhotosAfterDelete(userId)
        deleteMediaServerActor ! DeletePhotoFile(Seq(ImageInfo(photo.fileUrl.url.id, ImageType.photo, photo.userId.id)))
        photoAlbumWriteActor ! PhotoAlbumWriteActor.DeletePhotoAlbumIfEmpty(photo.photoAlbumId, userId)
        urlsDataActor ! UrlsDataActor.DeleteUrlByEntityAndUrl((photo.id.id, photo.fileUrl.url))
        SuccessfulResponse
      }
      result pipeTo sender

    case UpdateUserPhotosAfterDelete(userId) =>
      (photoDataActor ? GetLastUserPhotos(userId)).mapTo[Seq[Photo]].map {
        photos =>
          userDataActor ! UpdateUserPhotos(userId, photos map (_.id))
      }

    case LikePhoto(photoId, userId) =>
      for {
        photo <- getPhoto(photoId)
        like = PhotoLike(photoId = photoId, userId = userId, time = new Date)
        _ <- photoLikeDataActor ? Like(like)
      } yield {
        photoDataActor ! UpdateUserLikes(photoId, userId)
        Option(photo.userId).filterNot(_ == userId).foreach(actor => userEventWriteActor ! UserEventWriteActor.NewPhotoLikeReceived(like, actor))

      }
      sender ! SuccessfulResponse

    case DislikePhoto(photoId, userId) =>
      for {
        selfliked <- (photoLikeDataActor ? PhotoLikeDataActor.UserLikeExist(userId, photoId)).mapTo[Boolean]
        res <- photoLikeDataActor ? DeleteLike(photoId, userId)
        lastLikes <- (photoLikeDataActor ? GetLikes(photoId)).mapTo[List[PhotoLike]]
      } yield {
          if(selfliked)
            photoDataActor ! UpdateUserDislikes(photoId, lastLikes map (_.userId))
      }
      sender ! SuccessfulResponse

    case CommentPhoto(photoId, userId, photoMessage, dsign, lang) =>
      (for {
        photo <- getPhoto(photoId)
        _ <- IsWriteActionAllowed(userId, dsign, lang)
        photoComment = PhotoComment(photoObjectId = photoId.id, authorObjId = userId.id, time = new Date, message = photoMessage.message)
        comment <- addComment(photoComment)
      } yield {
        photoDataActor ! AddUserComment(photoId, userId)
        Option(photo.userId).filterNot(_ == userId).foreach(actor => userEventWriteActor ! UserEventWriteActor.NewPhotoCommentReceived(photoComment))
        PhotoCommentCreatedResponse(comment)
      }).pipeTo(sender)

    case DeletePhotoComment(userId, commentId) =>
      val result = for {
        photoComment <- (photoCommentsDataActor ? FindComment(commentId)).mapTo[PhotoComment]
        _ <- checkRightsToDeleteComment(photoComment, userId)
        _ <- photoCommentsDataActor ? DeleteComment(commentId)
        lastComments <- (photoCommentsDataActor ? GetLastPhotoComments(photoComment.photoId)).mapTo[Seq[PhotoComment]]
      } yield {
        photoDataActor ! DeleteUserComment(photoComment.photoId, lastComments map (_.authorId))
        SuccessfulResponse
      }
      result pipeTo sender

    case ChangePhoto(photoId, request, userId) =>
      photoDataActor ! UpdatePhoto(photoId, request)
      sender ! SuccessfulResponse
  }

  def checkRightsToDeleteComment(photoComment: PhotoComment, userId: UserId): Future[_] =
    if (photoComment.authorId == userId) Future successful ()
    else Future failed ForbiddenAppException(message = "Only the author of the photo comment is able to delete it")

  def getPhotoAlbumId(albumId: Option[PhotoAlbumId], userId: UserId, photoUrl: String, ctx: RouteContext): Future[PhotoAlbumId] = {
    albumId.map { albumId =>
      Future successful(albumId)
    } getOrElse {
      (photoAlbumDataActor ? PhotoAlbumDataActor.GetUserDefaultPhotoAlbumId(userId)).mapTo[Option[PhotoAlbumId]].flatMap { albumId =>
        albumId.map { albumId =>
          Future successful(albumId)
        } getOrElse {
          (photoAlbumWriteActor ? PhotoAlbumWriteActor.CreatePhotoAlbumIfNotExist(albumId, userId, photoUrl, ctx)).mapTo[PhotoAlbumId]
        }
      }
    }

  }

  def getPhoto(photoId: PhotoId) =
    photoDataActor.ask(PhotoDataActor.GetPhotoById(photoId)).mapTo[Photo]

  def IsWriteActionAllowed(userId: UserId, dsign: DigitalSign, lang: Lang) =
    cacheWriteSnifferActor.ask(CacheWriteSnifferActor.IsWriteActionAllowed(userId, dsign, lang))

  def addComment(photoComment: PhotoComment) =
    (photoCommentsDataActor ? AddComment(photoComment)).mapTo[PhotoComment]

}

case class PhotoCommentCreatedResponse(comment: PhotoCommentIdCreated) extends SuccessfulResponse

case class PhotoCommentIdCreated(id: PhotoCommentId)

object PhotoCommentCreatedResponse {

  def apply(comment: PhotoComment): PhotoCommentCreatedResponse = {
    PhotoCommentCreatedResponse(
      PhotoCommentIdCreated(comment.photoCommentId)
    )
  }

}