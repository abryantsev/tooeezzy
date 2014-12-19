package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.Present
import org.bson.types.ObjectId

trait PresentRepository extends MongoRepository[Present, ObjectId] {

  @Query(value = "{ id: { $in: ?0}}")
  def findPresents(ids: Seq[ObjectId]): java.util.List[Present]

  @Query(value = "{ uid: ?0, p.pid: ?1 }")
  def findUserPresentsByProduct(userId: ObjectId, productId: ObjectId): java.util.List[Present]
}