package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait CountryField extends HasIdentity

object CountryField extends HasIdentityFactoryEx[CountryField] {
  case object Id extends CountryField {
    def id = "id"
  }
  case object Name extends CountryField {
    def id = "name"
  }
  case object Phone extends CountryField {
    def id = "phone"
  }

  def values = Seq(Id, Name, Phone)
}