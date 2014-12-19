package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.Promotion

trait PromotionRepository extends MongoRepository[Promotion, ObjectId] {

  @Query("{ id: { $in : ?0 } }")
  def findByIds(ids: Seq[ObjectId]): java.util.List[Promotion]
}