package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait NamePopularitySortType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object NamePopularitySortType extends HasIdentityFactoryEx[NamePopularitySortType] {

  object Name extends NamePopularitySortType {
    def id = "name"
  }

  object Popularity extends NamePopularitySortType {
    def id = "popularity"
  }

  val values = Seq(Name, Popularity)
}
