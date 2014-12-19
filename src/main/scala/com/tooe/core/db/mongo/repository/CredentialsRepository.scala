package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.Credentials
import org.bson.types.ObjectId

trait CredentialsRepository  extends MongoRepository[Credentials, ObjectId]{

  @Query("{ userName : ?0, passwordHash : ?1 }")
  def getUserCredentials(userName:String, passwordHash: String):Credentials

  @Query("{ verificationKey : ?0}")
  def findUserCredentialsByVerificationKey(verificationKey:String):Credentials

  @Query("{ un: ?0 }")
  def findByLogin(login: String): Credentials
}