package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait PromotionVisitorsField extends HasIdentity

object PromotionVisitorsField extends HasIdentityFactoryEx[PromotionVisitorsField] {
  case object Users extends PromotionVisitorsField {
    def id = "users"
  }
  case object UsersCount extends PromotionVisitorsField {
    def id = "userscount"
  }
  case object FriendsCount extends PromotionVisitorsField {
    def id = "friendscount"
  }
  def values = Seq(Users, UsersCount, FriendsCount)
}