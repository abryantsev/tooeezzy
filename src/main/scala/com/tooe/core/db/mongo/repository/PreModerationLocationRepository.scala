package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.PreModerationLocation
import org.bson.types.ObjectId

trait PreModerationLocationRepository extends MongoRepository[PreModerationLocation, ObjectId] {


  @Query(fields = "{ns: 0}")
  override def findAll(): java.util.List[PreModerationLocation]

  @Query(fields = "{ns: 0}")
  override def findOne(id: ObjectId): PreModerationLocation

  @Query(value = "{ puid : ?0}", fields = "{ns: 0}")
  def findByLocationId(locationId: ObjectId): PreModerationLocation

}
