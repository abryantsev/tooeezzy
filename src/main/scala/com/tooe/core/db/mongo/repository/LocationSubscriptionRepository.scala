package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.LocationSubscription
import org.bson.types.ObjectId

trait LocationSubscriptionRepository extends MongoRepository[LocationSubscription, ObjectId] {

}
