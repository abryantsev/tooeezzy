package com.tooe.core.usecase.location_photo

import akka.actor.Actor
import com.tooe.core.util.{Images, ActorHelper}
import com.tooe.core.util.MediaHelper._
import com.tooe.core.application.{Actors, AppActors}
import akka.pattern._
import com.tooe.core.domain._
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor._
import com.tooe.core.db.mongo.domain._
import com.tooe.api.service._
import com.tooe.core.usecase._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.usecase.location_photo_like.LocationPhotoLikeDataActor
import com.tooe.core.usecase.location_photo_like.LocationPhotoLikeDataActor._
import com.tooe.core.usecase.location_photo_comment.LocationPhotoCommentDataActor
import com.tooe.core.usecase.location_photo_comment.LocationPhotoCommentDataActor._
import java.util.Date
import scala.concurrent.Future
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.usecase.UserReadActor.GetAuthorDetailsByIds

object LocationPhotoReadActor {
  final val Id = Actors.LocationPhotoRead

  case class GetLastSixUploadedPhoto(locationId: LocationId)
  case class GetLocationPhotoComments(locationPhotoId: LocationPhotoId, userId: UserId, offsetLimit: OffsetLimit)
  case class ShowLocationPhoto(photoId: LocationPhotoId, userId: UserId, viewType: ViewType)
  case class GetPhotoLikes(locationPhotoId: LocationPhotoId, userId: UserId, offsetLimit: OffsetLimit)
}


class LocationPhotoReadActor extends Actor with ActorHelper with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationPhotoReadActor._

  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val locationPhotoLikeDataActor = lookup(LocationPhotoLikeDataActor.Id)
  lazy val locationPhotoCommentDataActor = lookup(LocationPhotoCommentDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  def receive = {

    case GetLocationPhotoComments(locationPhotoId, userId, offsetLimit) =>
      val photoCommentDetailsFuture = locationPhotoCommentDataActor ? GetLocationPhotoCommentsCount(locationPhotoId) zip
        locationPhotoCommentDataActor ? GetPhotoComments(locationPhotoId, offsetLimit)
      val result = for {
        (count,comments) <- photoCommentDetailsFuture.mapTo[(Long, Seq[LocationPhotoComment])]
        userIds = comments.map(_.userId).toSet.toSeq
        userIdToAuthorDetailsMap <- getAuthorDetails(userIds)
      } yield GetLocationPhotosResponse(count, comments map CommentDetail(userIdToAuthorDetailsMap))
      result pipeTo sender

    case ShowLocationPhoto(photoId, userId, viewType) =>
      val photoAndSelfLikedFuture = (locationPhotoDataActor ? FindLocationPhoto(photoId)) zip (locationPhotoLikeDataActor ? UserLikeExist(photoId, userId))
      val result = photoAndSelfLikedFuture.mapTo[(LocationPhoto, Boolean)].flatMap { case (photo: LocationPhoto, selfLiked: Boolean) =>
          viewType match {
            case ViewType.Short => Future successful LocationPhotoResponse(LocationPhotoDetails(photo, selfLiked, Images.Locationphoto.Short.Media.Self))
            case ViewType.None => getPhotoFullDetails(photo, selfLiked)
        }
      }
      result pipeTo sender

    case GetPhotoLikes(locationPhotoId, userId, offsetLimit) =>
      val result = for {
        (likes, likesCount) <- (locationPhotoLikeDataActor ? GetLocationPhotoLikes(locationPhotoId, offsetLimit))
                              .zip(locationPhotoLikeDataActor ? GetLocationPhotoLikesCount(locationPhotoId)).mapTo[(Seq[LocationPhotoLike], Long)]

        (authors, selfLiked) <- (userReadActor ? GetAuthorDetailsByIds(likes.map(_.userId), Images.Photolikes.Full.Author.Media))
                            .zip(locationPhotoLikeDataActor ? UserLikeExist(locationPhotoId, userId)).mapTo[(Seq[AuthorDetails], Boolean)]
       } yield GetPhotoLikesResponse(likesCount, if(selfLiked) Some(true) else None, authors.map(LocationPhotoLikeDetails))

      result pipeTo sender

  }

  def getPhotoFullDetails(locationPhoto: LocationPhoto, selfLiked: Boolean) = {
    val likesUserIds = locationPhoto.usersLikesIds
    val lastCommentsFuture = locationPhotoCommentDataActor ? GetPhotoComments(locationPhoto.id, OffsetLimit(0, 20))
    val commentsUserIds = locationPhoto.comments.toSet.toSeq

    for {
      lastComments <- lastCommentsFuture.mapTo[Seq[LocationPhotoComment]]
      userIds = (likesUserIds ++ commentsUserIds).toSet.toSeq
      userIdToAuthorDetailsMap <- getAuthorDetails(userIds)
      likes = Some(likesUserIds.map { id =>  LikeDetails(userIdToAuthorDetailsMap(id))})
      comments = Some(lastComments map CommentDetail(userIdToAuthorDetailsMap))
    } yield LocationPhotoResponse(LocationPhotoDetails(locationPhoto, selfLiked,  Images.Locationphoto.Full.Media.Self, likes, comments))
  }

  def getAuthorDetails(userIds: Seq[UserId]): Future[Map[UserId, AuthorDetails]] =
    (userReadActor ? UserReadActor.GetAuthorDetailsByIds(userIds, Images.Locationphoto.Full.Actor.Media)).mapTo[Seq[AuthorDetails]].map(_.toMapId(_.id))
}

case class LocationPhotoCreated(@JsonProperty("locationphoto") locationPhotoId: LocationPhotoIdCreated) extends SuccessfulResponse

case class LocationPhotoIdCreated(@JsonProperty("id") photoId: LocationPhotoId)

case class GetLocationPhotosResponse(@JsonProperty("commentscount") commentsCount: Long, comments: Seq[CommentDetail]) extends SuccessfulResponse

case class CommentDetail(id: LocationPhotoCommentId,
                         time: Date,
                         msg: String,
                         author: AuthorDetails)

object CommentDetail {
  def apply(userIdToAuthorDetailsMap: Map[UserId, AuthorDetails])(comment: LocationPhotoComment): CommentDetail =
    CommentDetail(
      id = comment.id,
      msg = comment.message,
      time = comment.time,
      author = userIdToAuthorDetailsMap(comment.userId)
    )
}

case class LocationPhotoResponse(photo: LocationPhotoDetails) extends SuccessfulResponse

case class LocationPhotoDetails(id: LocationPhotoId,
                                name: String,
                                media: MediaUrl,
                                time: Date,
                                @JsonProperty("likescount") likesCount: Long,
                                @JsonProperty("selfliked") selfLiked: Boolean,
                                @JsonProperty("commentscount") commentsCount: Long,
                                likes: Option[Seq[LikeDetails]],
                                comments: Option[Seq[CommentDetail]])

object LocationPhotoDetails {
  def apply
  (
    locationPhoto: LocationPhoto,
    selfLiked: Boolean,
    mediaDimension: String,
    likes: Option[Seq[LikeDetails]] = None,
    comments: Option[Seq[CommentDetail]] = None
    ): LocationPhotoDetails
  = {
    LocationPhotoDetails(
      id = locationPhoto.id,
      name = locationPhoto.name getOrElse "",
      media = locationPhoto.fileUrl.asMediaUrl(mediaDimension),
      time = locationPhoto.creationDate,
      likesCount = locationPhoto.likesCount,
      selfLiked = selfLiked,
      commentsCount = locationPhoto.commentsCount,
      likes = likes,
      comments = comments
    )
  }
}

case class GetPhotoLikesResponse(@JsonProperty("likescount") likesCount: Long,
                                  @JsonProperty("selfliked") selfLiked: Option[Boolean],
                                  @JsonProperty("likes") likes: Seq[LocationPhotoLikeDetails]) extends SuccessfulResponse

case class LocationPhotoLikeDetails(author: AuthorDetails)