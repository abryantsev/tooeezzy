package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.LocationPhoto
import org.springframework.data.domain.Pageable

trait LocationPhotoRepository  extends MongoRepository[LocationPhoto, ObjectId] {

  @Query(value = "{ id: { $in: ?0}}")
  def getLocationPhotos(photoIds:Seq[ObjectId]): java.util.List[LocationPhoto]

  @Query(value = "{lid: ?0}")
  def findByLocationId(locationId: ObjectId, pageable: Pageable): java.util.List[LocationPhoto]

  @Query(value = "{pid: ?0}")
  def findByAlbumId(albumId: ObjectId): java.util.List[LocationPhoto]

  @Query(value = "{pid: ?0}")
  def findByAlbumId(albumId: ObjectId, pageable: Pageable): java.util.List[LocationPhoto]

}
