package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.AdminCredentials
import org.bson.types.ObjectId

trait AdminCredentialsRepository extends MongoRepository[AdminCredentials, ObjectId] {

  @Query("{ un : ?0, pwd : ?1 }")
  def getCredentials(userName:String, passwordHash: String): AdminCredentials

  @Query("{ un: ?0 }")
  def findByLogin(login: String): AdminCredentials
}
