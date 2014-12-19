package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.PromotionVisitor
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Query

trait PromotionVisitorRepository extends MongoRepository[PromotionVisitor, ObjectId] {
  @Query(value = "{ pid: ?0 }")
  def findAllVisitors(promotionId: ObjectId, p: Pageable): java.util.List[PromotionVisitor]

  @Query(value = "{ pid: ?0, uid: ?1 }")
  def find(promotionId: ObjectId, userId: ObjectId): java.util.List[PromotionVisitor]

  @Query(value = "{ pid: { $in : ?0 } }")
  def findByIds(promotionIds: java.util.Collection[ObjectId]): java.util.Collection[PromotionVisitor]

  @Query(value = "{ pid: ?0 }", fields = "{ uid: 1 }")
  def findAllVisitorUserIds(promotionId: ObjectId): java.util.Collection[PromotionVisitor]
}