package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import org.springframework.data.domain.Pageable

trait LocationPhotoAlbumRepository  extends MongoRepository[LocationPhotoAlbum, ObjectId] {
  @Query(value = "{ lid: ?0}")
  def findByLocationId(locatioId: ObjectId, pageable: Pageable): java.util.List[LocationPhotoAlbum]
}
