package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}

sealed trait FavoriteLocationField extends HasIdentity

object FavoriteLocationField extends HasIdentityFactoryEx[FavoriteLocationField] {
  case object Id extends FavoriteLocationField {
    def id = "id"
  }
  case object Name extends FavoriteLocationField {
    def id = "name"
  }
  case object Promotion extends FavoriteLocationField {
    def id = "promotion"
  }
  case object Category extends FavoriteLocationField {
    def id = "category"
  }
  case object Coords extends FavoriteLocationField {
    def id = "coords"
  }
  case object Media extends FavoriteLocationField {
    def id = "media"
  }
  case object Address extends FavoriteLocationField {
    def id = "address"
  }
  case object OpeningHours extends FavoriteLocationField {
    def id = "openinghours"
  }

  def values = Seq(Id, Name, Promotion, Category, Coords, Media, Address, OpeningHours)
}