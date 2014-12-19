package com.tooe.core.usecase.checkin

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}


sealed trait CheckinSearchSortType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object CheckinSearchSortType extends HasIdentityFactoryEx[CheckinSearchSortType] {

  object Name extends CheckinSearchSortType {
    def id = "name"
  }

  object Distance extends CheckinSearchSortType {
    def id = "distance"
  }

  val values = Seq(Name, Distance)

}