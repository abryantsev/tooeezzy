package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait UserFields extends HasIdentity

object UserFields extends HasIdentityFactoryEx[UserFields]{
  case object Users extends UserFields {
    def id = "users"
  }
  case object UsersCount extends UserFields {
    def id = "userscount"
  }

  def values = Seq(Users, UsersCount)
}
