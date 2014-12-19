package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.{Coordinates, RegionId, CountryId, UserId}
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "favorites_stats")
case class FavoriteStats
(id: FavoriteStatsId = FavoriteStatsId(),
 userId: UserId,
 countryId: CountryId,
 favoritesCount: Int,
 regions: Seq[FavoritesInRegion],
 countryCoordinates: Coordinates)

case class FavoriteStatsId(id: ObjectId = new ObjectId)
case class FavoritesInRegion(region: RegionId, count: Int)