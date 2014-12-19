package com.tooe.core.db.mongo.repository

import com.tooe.core.db.mongo.domain.FriendshipRequest
import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable

trait FriendshipRequestRepository extends MongoRepository[FriendshipRequest, ObjectId] {
  @Query("{ uid: ?0 }")
  def findByUserId(userId: ObjectId, p: Pageable): java.util.List[FriendshipRequest]

  @Query("{ uid: ?0, aid: ?1 }")
  def find(userId: ObjectId, actorId: ObjectId): java.util.List[FriendshipRequest]
}