package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait LocationEntityField extends HasIdentity

object LocationEntityField extends HasIdentityFactoryEx[LocationEntityField] {
  case object Id extends LocationEntityField {
    def id = "id"
  }
  case object Name extends LocationEntityField {
    def id = "n"
  }
  case object OpeningHours extends LocationEntityField {
    def id = "oh"
  }
  case object Address extends LocationEntityField {
    def id = "c"
  }
  case object Media extends LocationEntityField {
    def id = "lm"
  }
  case object Category extends LocationEntityField {
    def id = "lc"
  }
  case object Promotion extends LocationEntityField {
    def id = "pf"
  }
  case object Statistics extends LocationEntityField {
    def id = "st"
  }

  def values = Seq(Id, Name, OpeningHours, Address, Media, Category, Promotion, Statistics)
}