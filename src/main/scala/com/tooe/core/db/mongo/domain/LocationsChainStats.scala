package com.tooe.core.db.mongo.domain

import org.bson.types.ObjectId
import com.tooe.core.domain.{Coordinates, RegionId, CountryId, LocationsChainId}
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "locationschain_stats")
case class LocationsChainStats
(id: LocationsChainStatsId = LocationsChainStatsId(),
 chainId: LocationsChainId,
 countryId: CountryId,
 locationsCount: Int,
 regions: Seq[LocationsInRegion],
 coordinates: Coordinates)

case class LocationsChainStatsId(id: ObjectId = new ObjectId())
case class LocationsInRegion(region: RegionId, count: Int)
