package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentity, HasIdentityFactoryEx}

sealed trait StarStatisticField extends HasIdentity

object StarStatisticField extends HasIdentityFactoryEx[StarStatisticField] {

  case object WishesCount extends StarStatisticField {
    def id = "wishes"
  }
  case object PhotoAlbumsCount extends StarStatisticField {
    def id = "photoalbums"
  }
  case object FavoriteLocationsCount extends StarStatisticField {
    def id = "favorites"
  }
  case object SubscribersCount extends StarStatisticField {
    def id = "subscribers"
  }

  def values = Seq(WishesCount, PhotoAlbumsCount, FavoriteLocationsCount, SubscribersCount)

}