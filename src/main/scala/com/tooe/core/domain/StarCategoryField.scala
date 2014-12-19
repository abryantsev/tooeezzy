package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait StarCategoryField extends HasIdentity

object StarCategoryField extends HasIdentityFactoryEx[StarCategoryField] {
  case object Id extends StarCategoryField {
    def id = "id"
  }
  case object Name extends StarCategoryField {
    def id = "name"
  }
  case object Description extends StarCategoryField {
    def id = "description"
  }
  case object Media extends StarCategoryField {
    def id = "media"
  }
  case object StarsCounter extends StarCategoryField {
    def id = "starscounter"
  }

  def values = Seq(Id, Name, Description, Media, StarsCounter)
}