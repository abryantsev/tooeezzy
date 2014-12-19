package com.tooe.core.usecase.location

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait ModerationLocationSearchSortType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object ModerationLocationSearchSortType extends HasIdentityFactoryEx[ModerationLocationSearchSortType] {

  object Name extends ModerationLocationSearchSortType {
    def id = "name"
  }

  object ModerationStatus extends ModerationLocationSearchSortType {
    def id = "modstatus"
  }

  val values = Seq(Name, ModerationStatus)

}