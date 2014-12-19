package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{LocationSubscriptionId, UserId, LocationId}

@Document(collection = "location_subscription")
case class LocationSubscription(id: LocationSubscriptionId = LocationSubscriptionId(), userId: UserId, locationId: LocationId)
