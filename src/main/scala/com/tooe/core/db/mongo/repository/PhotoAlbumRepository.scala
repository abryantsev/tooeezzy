package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.PhotoAlbum
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable

trait PhotoAlbumRepository extends MongoRepository[PhotoAlbum, ObjectId]{

  @Query("{ uid: ?0 }")
  def findByUserObjectId(userId: ObjectId, pageable: Pageable): java.util.List[PhotoAlbum]

}
