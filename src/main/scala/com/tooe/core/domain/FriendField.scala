package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait FriendField extends HasIdentity

object FriendField extends HasIdentityFactoryEx[FriendField] {
  case object Users extends FriendField {
    def id = "users"
  }
  case object UsersCount extends FriendField {
    def id = "userscount"
  }

  def values = Seq(Users, UsersCount)
}