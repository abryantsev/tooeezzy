package com.tooe.core.db.mongo.domain

import org.bson.types.ObjectId
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{LocationCategoryId, Coordinates, RegionId, CountryId}
import com.tooe.core.util.HashHelper

@Document(collection = "region")
case class Region
(
  id: RegionId = RegionId(new ObjectId),
  countryId: CountryId = CountryId(HashHelper.uuid),
  name: ObjectMap[String],
  isCapital: Option[Boolean],
  coordinates: Coordinates,
  statistics: Statistics = Statistics()
  ) extends UnmarshallerEntity

class RegionLocationCategories(reg: Region) {
  def statistics = new {
    def locationCategories: Seq[LocationCategoryId] = reg.statistics.locationCategories
  }
}