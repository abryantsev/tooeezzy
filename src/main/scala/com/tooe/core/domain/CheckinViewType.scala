package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait CheckinViewType extends HasIdentity

object CheckinViewType extends HasIdentityFactoryEx[CheckinViewType] {
  case object FriendsShort extends CheckinViewType {
    def id = "friends(short)"
  }
  case object UsersShort extends CheckinViewType {
    def id = "users(short)"
  }
  case object FriendsCount extends CheckinViewType {
    def id = "friendscount"
  }
  case object UsersCount extends CheckinViewType {
    def id = "userscount"
  }

  def values = Seq(FriendsShort, UsersShort, FriendsCount, UsersCount)
}