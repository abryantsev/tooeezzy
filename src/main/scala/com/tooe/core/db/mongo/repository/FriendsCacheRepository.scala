package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.{Credentials, FriendsCache}
import org.bson.types.ObjectId

trait FriendsCacheRepository  extends MongoRepository[FriendsCache, ObjectId] {

  @Query("{ uid : ?0, gid : ?1 }")
  def findFriendsInCache(userId: ObjectId, friendsGroup: String) : FriendsCache
}
