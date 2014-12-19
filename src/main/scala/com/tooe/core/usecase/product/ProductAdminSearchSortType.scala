package com.tooe.core.usecase.product

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}

sealed trait ProductAdminSearchSortType extends HasIdentity with UnmarshallerEntity {
  def id: String
}

object ProductAdminSearchSortType extends HasIdentityFactoryEx[ProductAdminSearchSortType] {

  object Name extends ProductAdminSearchSortType {
    def id = "name"
  }

  object LocationName extends ProductAdminSearchSortType {
    def id = "locationname"
  }

  object Count extends ProductAdminSearchSortType {
    def id = "count"
  }

  object Status extends ProductAdminSearchSortType {
    def id = "status"
  }

  val values = Seq(Name, LocationName, Count, Status)

  val Default = Name

}
