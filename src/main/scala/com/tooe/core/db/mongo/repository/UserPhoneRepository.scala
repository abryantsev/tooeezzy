package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.UserPhone
import org.bson.types.ObjectId

trait UserPhoneRepository extends MongoRepository[UserPhone, ObjectId]{
  @Query(value = "{ p.c: ?0, p.n : ?1 }")
  def findUserPhoneByCountryCodeAndNumber(countryCode: String, phoneNumber:String): UserPhone
}
