package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait StarField extends HasIdentity

object StarField extends HasIdentityFactoryEx[StarField] {
  case object Id extends StarField {
    def id = "id"
  }
  case object Name extends StarField {
    def id = "name"
  }
  case object Lastname extends StarField {
    def id = "lastname"
  }
  case object Address extends StarField {
    def id = "address"
  }
  case object Media extends StarField {
    def id = "media"
  }
  case object Subscribers extends StarField {
    def id = "subscriberscount"
  }

  def values = Seq(Id, Name, Lastname, Address, Media, Subscribers)
}