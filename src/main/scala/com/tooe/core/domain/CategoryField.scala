package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait CategoryField extends HasIdentity

object CategoryField extends HasIdentityFactoryEx[CategoryField] {
  case object Id extends CategoryField {
    def id = "id"
  }
  case object Name extends CategoryField {
    def id = "name"
  }
  case object Media extends CategoryField {
    def id = "media"
  }

  def values = Seq(Id, Name, Media)
}