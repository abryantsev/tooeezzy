package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.Wish
import org.springframework.data.domain.Pageable

trait WishRepository extends MongoRepository[Wish, ObjectId] {
  import WishRepository._

  @Query(value = searchUsersWishesQuery)
  def searchUsersWishes(userId: ObjectId, fulfilledOnly: Boolean, p: Pageable): java.util.List[Wish]

  @Query("{ uid: ?0, p.pid: ?1 }")
  def findWishByProduct(userId: ObjectId, productId: ObjectId): Wish
}

object WishRepository {
  final val searchUsersWishesQuery = "{ uid: ?0, ft: { $exists: ?1 } }"
}