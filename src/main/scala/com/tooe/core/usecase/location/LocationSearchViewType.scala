package com.tooe.core.usecase.location

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}
import com.tooe.core.domain.LocationEntityField


sealed trait LocationSearchViewType extends HasIdentity with UnmarshallerEntity{
  def id: String
  def fields: Set[LocationEntityField]
}

object LocationSearchViewType extends HasIdentityFactoryEx[LocationSearchViewType] {
  import LocationEntityField._
  private val shortViewFields: Set[LocationEntityField] = Set(Id, Name, OpeningHours, Address, Media, Promotion, Category)

  object Locations extends LocationSearchViewType {
    def id = "locations"

    def fields: Set[LocationEntityField] = shortViewFields ++ Set(Statistics)
  }

  object LocationsCount extends LocationSearchViewType {
    def id = "locationscount"

    def fields: Set[LocationEntityField] = Set()
  }

  val values = Seq(Locations, LocationsCount)

}
