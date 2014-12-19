package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.Region
import org.bson.types.ObjectId

trait RegionRepository  extends MongoRepository[Region, ObjectId] {

  @Query("{cid : ?0 }")
  def findRegionsByCountryId(countryId: String): java.util.List[Region]

  @Query(value = "{ id : ?0 }", fields = "{ st : 1 }")
  def getRegionLocationCategories(regionId: ObjectId): Region

//  @Query("{'countryId' : ?0, ?1 : ?2 }")
//  def findRegion(countryId:ObjectId, fieldName: String, fieldValue: String): Region
}
