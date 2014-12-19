package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.PhotoComment
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable

trait PhotoCommentRepository extends MongoRepository[PhotoComment, ObjectId] {

  def findByPhotoObjectId(photoId: ObjectId, pageable: Pageable): java.util.List[PhotoComment]

}