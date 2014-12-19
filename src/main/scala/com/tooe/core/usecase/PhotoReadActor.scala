package com.tooe.core.usecase

import akka.actor.Actor
import akka.pattern.ask
import com.tooe.api.service._
import com.tooe.core.application.{Actors, AppActors}
import akka.pattern.pipe
import com.tooe.core.domain._
import com.tooe.core.usecase.PhotoDataActor._
import com.tooe.core.db.mongo.domain.{PhotoComment, Photo}
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.core.usecase.PhotoCommentDataActor._
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.domain.PhotoId
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor._
import com.tooe.api.boot.DefaultTimeout
import concurrent.Future
import com.tooe.core.util.Images
import com.tooe.core.util.MediaHelper._
import com.tooe.api.JsonProp

object PhotoReadActor {
  final val Id = Actors.PhotoRead

  case class GetPhoto(photoId: PhotoId, userId: UserId, viewType: ViewType)

  case class GetPhotoComments(photoId: PhotoId, userId: UserId, offsetLimit: OffsetLimit)

  case class GetMediaItems(imageIds: Seq[PhotoId])

  case class MediaUrlMap(map: Map[ImageInfo, MediaUrl])
}

class PhotoReadActor extends Actor with AppActors with DefaultTimeout {

  import PhotoReadActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val photoCommentsDataActor = lookup(PhotoCommentDataActor.Id)
  lazy val photoLikeDataActor = lookup(PhotoLikeDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  def receive = {

    case GetPhoto(photoId, userId, viewType) =>
      val photoAndSelfLikedFuture = (photoDataActor ? GetPhotoById(photoId)) zip (photoLikeDataActor ? UserLikeExist(userId, photoId))
      val result = photoAndSelfLikedFuture.mapTo[(Photo, Boolean)].flatMap { case (photo: Photo, selfLiked: Boolean) =>
        viewType match {
          case ViewType.Short => getAuthorDetails(photo.userId, Images.Photo.Short.Author.Media).map(authorDetails =>
            PhotoResponse(PhotoDetails(photo, authorDetails, selfLiked, None, None, ViewType.Short)))
          case ViewType.None => getAuthorDetails(photo.userId, Images.Photo.Full.Author.Media).flatMap(authorDetails =>
            getFullPhotoInfo(photo, authorDetails, selfLiked))
        }
      }
      result pipeTo sender

    case GetPhotoComments(photoId, userId, offsetLimit) =>
      val commentsAndCountFuture = (photoCommentsDataActor ? GetComments(photoId, offsetLimit)) zip (photoCommentsDataActor ? GetCommentCount(photoId))
      val result = for {
        (comments, commentsCount) <- commentsAndCountFuture.mapTo[(Seq[PhotoComment], Long)]
        commentsUserIds = comments.map(_.authorId).toSet
        authorDetailsMap <- getAuthorDetailsMap(commentsUserIds.toSeq, Images.Photocomments.Full.Author.Media)
      } yield PhotoCommentsResponse(comments, commentsCount, authorDetailsMap)
      result pipeTo sender

    case GetMediaItems(photoIds) =>
      val future = for {
        photos <- getPhotos(photoIds)
      } yield photos map (photo => MediaItemDto(photo.id.id, photo.fileUrl))
      future pipeTo sender
  }

  def getPhotos(photoIds: Seq[PhotoId]): Future[Seq[Photo]] =
    (photoDataActor ? PhotoDataActor.GetPhotos(photoIds)).mapTo[Seq[Photo]]

  def getFullPhotoInfo(photo: Photo, author: AuthorDetails, selfLiked: Boolean) =
    for {
      lastComments <- (photoCommentsDataActor ? GetLastPhotoComments(photo.id)).mapTo[Seq[PhotoComment]]
      likedUsers <- getAuthorDetailsMap(photo.usersLikesIds, Images.Photo.Full.Likes.Author)
      commentedUsers <- getAuthorDetailsMap(photo.usersCommentsIds, Images.Photo.Full.Comments.Author)
      userIdToAuthorDetailsMap = likedUsers ++ commentedUsers
      likeDetails = Some(photo.usersLikesIds map LikeDetails(userIdToAuthorDetailsMap))
      commentDetails = Some(lastComments map CommentFullDetail(userIdToAuthorDetailsMap))
    } yield PhotoResponse(PhotoDetails(photo, author, selfLiked, likeDetails, commentDetails))

  def getAuthorDetails(userId: UserId, imageSize: String): Future[AuthorDetails] = getAuthorDetailsMap(Seq(userId), imageSize) map (_(userId))

  def getAuthorDetailsMap(userIds: Seq[UserId], imageSize: String): Future[Map[UserId, AuthorDetails]] =
    (userReadActor ? UserReadActor.GetAuthorDetailsByIds(userIds, imageSize)).mapTo[Seq[AuthorDetails]] map (_ toMapId (_.id))
}

case class AddPhotoParameters(photoFormat: Option[PhotoFormat], file: String, photoAlbumId: Option[PhotoAlbumId], name: Option[String])

case class PhotoCreated(photo: PhotoCreatedId) extends SuccessfulResponse

case class PhotoCreatedId(id: PhotoId)

case class PhotoResponse(photo: PhotoDetails) extends SuccessfulResponse

case class PhotoDetails
(
  id: PhotoId,
  name: String,
  media: MediaUrl,
  time: Date,
  @JsonProp("likescount") likesCount: Int,
  @JsonProp("commentscount") commentsCount: Int,
  author: AuthorDetails,
  @JsonProp("selfliked") selfLiked: Option[Boolean],
  likes: Option[Seq[LikeDetails]],
  comments: Option[Seq[CommentFullDetail]]
  )

object PhotoDetails {
  def apply
  (
    photo: Photo,
    authorDetails: AuthorDetails,
    selfLiked: Boolean,
    likeDetails: Option[Seq[LikeDetails]] = None,
    commentDetails: Option[Seq[CommentFullDetail]] = None,
    viewType: ViewType = ViewType.None): PhotoDetails
  = {
    val imageSize = if(viewType == ViewType.None) Images.Photo.Full.Self.Media else Images.Photo.Short.Self.Media
    PhotoDetails(
      id = photo.id,
      name = photo.name getOrElse "",
      media =  photo.fileUrl.asMediaUrl(imageSize),
      time = photo.createdAt,
      likesCount = photo.likesCount,
      commentsCount = photo.commentsCount,
      author = authorDetails,
      selfLiked = if(selfLiked) Some(true) else None,
      likes = likeDetails,
      comments = commentDetails
    )
  }
}

case class CommentDetail
(
  time: Date,
  msg: String,
  author: AuthorDetails
  )

object CommentDetail {
  def apply(authorDetailsMap: Map[UserId, AuthorDetails])(comment: PhotoComment): CommentDetail =
    CommentDetail(
      msg = comment.message,
      time = comment.time,
      author = authorDetailsMap(comment.authorId)
    )
}

case class CommentFullDetail
(
  id: PhotoCommentId,
  time: Date,
  msg: String,
  author: AuthorDetails
  )

object CommentFullDetail {
  def apply(authorDetailsMap: Map[UserId, AuthorDetails])(comment: PhotoComment): CommentFullDetail =
    CommentFullDetail(
      id = comment.photoCommentId,
      msg = comment.message,
      time = comment.time,
      author = authorDetailsMap(comment.authorId)
    )
}

case class PhotoCommentsResponse
(
  @JsonProperty("commentscount") count: Long,
  comments: Seq[CommentFullDetail]
  ) extends SuccessfulResponse

object PhotoCommentsResponse {
  def apply(comments: Seq[PhotoComment], commentsCount: Long, authorDetailsMap: Map[UserId, AuthorDetails]): PhotoCommentsResponse =
    PhotoCommentsResponse(
      count = commentsCount,
      comments = comments map CommentFullDetail(authorDetailsMap)
    )
}

case class UserEventPhotoItem
(
  @JsonProperty("id") id: PhotoId,
  @JsonProperty("name") name: String,
  @JsonProperty("media") media: MediaUrl
  )

object UserEventPhotoItem {
  def apply(entity: Photo): UserEventPhotoItem = UserEventPhotoItem(
    id = entity.id,
    name = entity.name getOrElse "",
    media = entity.fileUrl.asMediaUrl(Images.Userevents.Full.Photo.Media)
  )
}