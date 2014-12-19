package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.Photo
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable

trait PhotoRepository extends MongoRepository[Photo, ObjectId] {

  @Query("{ pa : ?0 }")
  def findByPhotoAlbumObjectId(photoAlbumId: ObjectId): java.util.List[Photo]

  @Query("{ pa : ?0 }")
  def findByPhotoAlbumObjectId(photoAlbumId: ObjectId, pageable: Pageable): java.util.List[Photo]

  @Query("{ uid : ?0 }")
  def findByUserId(userId: ObjectId, pageable: Pageable): java.util.List[Photo]

}
