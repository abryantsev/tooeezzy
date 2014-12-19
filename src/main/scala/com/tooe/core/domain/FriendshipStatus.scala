package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait FriendshipStatus extends HasIdentity

object FriendshipStatus extends HasIdentityFactoryEx[FriendshipStatus] {
  case object IsInvited extends FriendshipStatus {
    def id = "isinvited"
  }
  case object InviteMe extends FriendshipStatus {
    def id = "inviteme"
  }

  case object Friend extends FriendshipStatus {
    def id = "friend"
  }

  val values = Seq(IsInvited, Friend, InviteMe)
}