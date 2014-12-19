package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{StarSubscriptionId, UserId}

@Document(collection = "star_subscription")
case class StarSubscription(
                             id: StarSubscriptionId = StarSubscriptionId(),
                              userId: UserId,
                              starId: UserId)
