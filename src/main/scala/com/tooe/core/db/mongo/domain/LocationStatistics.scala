package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.domain.{LocationId, LocationStatisticsId}

@Document( collection = "location_statistics" )
case class LocationStatistics
(
   id: LocationStatisticsId = LocationStatisticsId(),
   locationId: LocationId,
   registrationDate: Date,
   visitorsCount: Int
)
