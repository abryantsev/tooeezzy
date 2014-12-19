package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.util.Lang
import com.fasterxml.jackson.annotation.JsonProperty

@Document(collection = "userevent")
case class UserEvent
(
  id: UserEventId = UserEventId(),
  userId: UserId,
  eventTypeId: UserEventTypeId,
  createdAt: Date,
  status: Option[UserEventStatus] = None,
  actorId: Option[UserId] = None,
  //TODO like: Option[UserEventLike]
  //TODO c(message | comment | commentreplay)
  promotionInvitation: Option[PromotionInvitation] = None,
  invitation: Option[Invitation] = None,
  friendshipInvitation: Option[FriendshipInvitation] = None,
  present: Option[UserEventPresent] = None,
  newsLike: Option[UserEventNewsLike] = None,
  photoLike: Option[UserEventPhotoLike] = None,
  photoComment: Option[UserEventPhotoComment] = None,
  usersComment: Option[UserEventComment] = None
  ) {
  import UserEventTypeId._

  def getMessage: Option[String] = eventTypeId match {
    case InviteToPromotion => promotionInvitation flatMap (_.message)
    case Invite            => invitation flatMap (_.message)
    case Present           => present flatMap (_.message)
    case PhotoComment      => photoComment map (_.message)
    case UsersComment      => usersComment map (_.message)
    case NewsComment      => usersComment map (_.message)
    case ReplyNewsComment      => usersComment map (_.message)
    case _                 => None
  }

  def getLocationId: Option[LocationId] = eventTypeId match {
    case InviteToPromotion | InviteToPromotionReply | InviteToPromotionRejection => promotionInvitation map (_.locationId)
    case Invite | InvitationReply | InvitationRejection                          => invitation map (_.locationId)
    case Present                                                                 => present map (_.locationId)
    case _                                                                       => None
  }

  def getDate: Option[Date] = eventTypeId match {
    case Invite | InvitationReply | InvitationRejection => invitation flatMap (_.dateTime)
    case _                                              => None
  }

  def getPhotoIdOpt: Option[PhotoId] = eventTypeId match {
    case PhotoLike    => photoLike map (_.photoId)
    case PhotoComment => photoComment map (_.photoId)
    case _            => None
  }

  def getNewsId: Option[NewsId] = eventTypeId match {
    case NewsComment  => usersComment map (_.newsId)
    case ReplyNewsComment  => usersComment map (_.newsId)
    case NewsLike  => newsLike map (_.newsId)
    case _            => None
  }

  def getStatus: Option[UserEventStatus] = eventTypeId match {
    case Invite | InviteToPromotion | InviteToFriends => status
    case _ => None
  }

}

case class PromotionInvitation
(
  locationId: LocationId,
  promotionId: PromotionId,
  message: Option[String]
  )

case class Invitation
(
  locationId: LocationId,
  message: Option[String],
  dateTime: Option[Date]
  )

case class UserEventPresent
(
  presentId: PresentId,
  productId: ProductId,
  locationId: LocationId,
  message: Option[String] = None
  )

case class UserEventNews
(
  @JsonProperty("id")newsId: NewsId,
  @JsonProperty("type")newsTypeId: NewsTypeId,
  header: String
)

object UserEventNews {
  def apply(userEvent: UserEvent, eventTypeMap: Map[EventTypeId, EventType], usersMap: Map[UserId, User])(implicit lang: Lang): UserEventNews = {
    val actor = usersMap(userEvent.actorId.get)
    val recipient = usersMap(userEvent.userId)

    import UserEventTypeId._
    def actionName(gender: Gender): UserEventTypeId => String = {
      case UserEventTypeId.NewsLike => if (gender == Gender.Male) "оценил" else "оценила"
      case UserEventTypeId.NewsComment | UserEventTypeId.PhotoComment => if (gender == Gender.Male) "прокомментировал" else "прокомментировала"
      case ReplyNewsComment => if (gender == Gender.Male) "ответил" else "ответила"
      case _ => ""
    }
    val eventType = eventTypeMap(userEvent.eventTypeId)
    val header = eventType.userEventMessage.localized.getOrElse("")
      .format(getRepresentationName(actor), actionName(actor.gender)(eventType.id), getRepresentationName(recipient))
    val eventTypeId = UserEventTypeId.userEventTypeId2EventTypeId(userEvent.eventTypeId)
    UserEventNews(userEvent.getNewsId.get, eventTypeId, header)
  }

  def getRepresentationName(user: User) = user.name + " " + user.lastName
}

case class UserEventNewsLike (newsId: NewsId)

case class UserEventPhotoLike (photoId: PhotoId)
case class UserEventPhotoComment(message: String, photoId: PhotoId)

case class UserEventComment
(
  message: String,
  newsId: NewsId,
  originalMessage: Option[String]
  )

case class FriendshipInvitation(id: FriendshipRequestId)