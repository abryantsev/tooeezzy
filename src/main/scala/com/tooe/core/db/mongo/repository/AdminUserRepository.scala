package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.{User, AdminUser}
import org.bson.types.ObjectId

trait AdminUserRepository extends MongoRepository[AdminUser, ObjectId] {

  @Query(fields = "{ns: 0}")
  override def findAll(): java.util.List[AdminUser]

  @Query(fields = "{ns: 0}")
  override def findOne(id: ObjectId): AdminUser

  @Query(value = "{ id: ?0 }", fields = "{ r: 1 }")
  def getRole(userId: ObjectId): AdminUser
}
