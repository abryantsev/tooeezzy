package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait UserField extends HasIdentity

object UserField extends HasIdentityFactoryEx[UserField] {
  case object Id extends UserField {
    def id = "id"
  }
  case object Name extends UserField {
    def id = "name"
  }
  case object Lastname extends UserField {
    def id = "lastname"
  }
  case object Address extends UserField {
    def id = "address"
  }
  case object Media extends UserField {
    def id = "media"
  }

  def values = Seq(Id, Name, Lastname, Address, Media)
}