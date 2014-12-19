package com.tooe.core.domain

case class UserEventTypeId(id: String)

object UserEventTypeId {
  val InviteToPromotion = UserEventTypeId("promoinvitation")
  val InviteToPromotionReply = UserEventTypeId("replypromoinvitation")
  val InviteToPromotionRejection = UserEventTypeId("rejectpromoinvitation")

  val Present = UserEventTypeId("present")
  val InviteToFriends = UserEventTypeId("invitetofriends")
  val FriendshipConfirmation = UserEventTypeId("friend")

  val Invite = UserEventTypeId("invitation")
  val InvitationReply = UserEventTypeId("replyinvitation")
  val InvitationRejection = UserEventTypeId("rejectinvitation")

  val NewsLike = UserEventTypeId("newslike")
  val PhotoLike = UserEventTypeId("photolike")
  val PhotoComment = UserEventTypeId("photocomment")
  val UsersComment = UserEventTypeId("userscomment")
  val NewsComment = UserEventTypeId("newscomment")
  val ReplyNewsComment = UserEventTypeId("replynewscomment")


  implicit def userEventTypeId2EventTypeId(event: UserEventTypeId) = EventTypeId(event.id)
  implicit def eventTypeId2UserEventTypeId(event: EventTypeId) = UserEventTypeId(event.id)
}