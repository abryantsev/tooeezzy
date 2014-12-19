package com.tooe.core.usecase.location

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait PreModerationLocationAdminSearchSortType extends HasIdentity with UnmarshallerEntity

object PreModerationLocationAdminSearchSortType extends HasIdentityFactoryEx[PreModerationLocationAdminSearchSortType] {

  object Name extends PreModerationLocationAdminSearchSortType {
    def id = "name"
  }

  object Company extends PreModerationLocationAdminSearchSortType {
    def id = "company"
  }

  object ModerationStatus extends PreModerationLocationAdminSearchSortType {
    def id = "modstatus"
  }

  val Default = Name

  def values = Seq(Name, Company, ModerationStatus)

}