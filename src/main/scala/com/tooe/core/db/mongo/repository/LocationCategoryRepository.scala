package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.{LocationCategory, Product}

trait LocationCategoryRepository  extends MongoRepository[LocationCategory, String] {

  @Query(value = "{ id: { $in: ?0}}")
  def getLocationsCategories(locationIds:Seq[String]): java.util.List[LocationCategory]
}
