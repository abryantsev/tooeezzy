package com.tooe.core.domain

import com.fasterxml.jackson.annotation.JsonProperty
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.{Region, LocationAddress}

case class LocationFullAddressItem
(
  @JsonProperty("country") country: String,
  @JsonProperty("countryid") countryId: String,
  @JsonProperty("region") region: String,
  @JsonProperty("regionid") regionId: ObjectId,
  @JsonProperty("street") street: String
  )

object LocationFullAddressItem {
  def apply(la: LocationAddress, region: Region): LocationFullAddressItem = LocationFullAddressItem(
    country = la.country,
    region = la.regionName,
    street = la.street,
    regionId = la.regionId.id,
    countryId = region.countryId.id
  )
}