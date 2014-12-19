package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.LocationPhotoComment
import org.springframework.data.domain.Pageable

trait LocationPhotoCommentRepository extends MongoRepository[LocationPhotoComment, ObjectId] {

  @Query("{ pid: ?0 }")
  def findByLocationPhotoId(photoId: ObjectId, pageable: Pageable): java.util.List[LocationPhotoComment]

}
