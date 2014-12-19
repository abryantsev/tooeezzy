package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.PhotoLike
import org.springframework.data.domain.Pageable

trait PhotoLikeRepository extends MongoRepository[PhotoLike, ObjectId] {

  @Query("{ pid: ?0 }")
  def findByPhotoId(photoId: ObjectId, pageable: Pageable): java.util.List[PhotoLike]

}
