package com.tooe.core.db.mongo.repository

import com.tooe.core.db.mongo.domain.StarSubscription
import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.domain.UserId
import org.bson.types.ObjectId

trait StarSubscriptionRepository extends MongoRepository[StarSubscription, ObjectId] {

  @Query(value = "{ uid: ?0}")
  def findByUser(userId: ObjectId): java.util.List[StarSubscription]

}
