package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.Company
import org.bson.types.ObjectId

trait CompanyRepository extends MongoRepository[Company, ObjectId] {

  @Query(value = "{ aid: ?0 }", fields = "{ _id : 1 }")
  def findCompaniesByAgentUserId(objectId: ObjectId): java.util.Collection[Company]
}