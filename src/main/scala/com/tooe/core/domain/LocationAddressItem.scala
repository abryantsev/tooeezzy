package com.tooe.core.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.domain.LocationAddress

case class LocationAddressItem
(
  @JsonProperty("country") country: String,
  @JsonProperty("region") region: String,
  @JsonProperty("street") street: String
  )

object LocationAddressItem {
  def apply(la: LocationAddress): LocationAddressItem = LocationAddressItem(
    country = la.country,
    region = la.regionName,
    street = la.street
  )
}
