package com.tooe.core.db.mongo.repository

import com.tooe.core.db.mongo.domain.LocationPhotoLike
import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable

trait LocationPhotoLikeRepository extends MongoRepository[LocationPhotoLike, ObjectId] {

  @Query("{ pid: ?0 }")
  def findByLocationPhotoId(locationPhotoId: ObjectId, pageable: Pageable): java.util.List[LocationPhotoLike]

}
