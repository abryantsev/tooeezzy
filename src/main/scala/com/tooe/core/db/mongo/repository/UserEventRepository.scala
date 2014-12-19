package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.UserEvent
import org.springframework.data.domain.Pageable

trait UserEventRepository extends MongoRepository[UserEvent, ObjectId] {

  @Query("{ uid: ?0 }")
  def findByUserId(userId: ObjectId, p: Pageable): java.util.List[UserEvent]

  @Query("{ if: { fid: ?0 } }")
  def findByFriendshipRequestId(friendshipRequestId: ObjectId): java.util.List[UserEvent]
}