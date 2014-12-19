package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.User
import org.bson.types.ObjectId

trait UserRepository extends MongoRepository[User, ObjectId]{

  @Query(value = "{ c.e: ?0 }", fields = "{ id: 1 }")
  def findUserByEmail(email: String): User

  @Query(value = "{ id: ?0 }", fields = "{ c.p: 1 }")
  def findUserPhones(userId: ObjectId): User

  @Query(value = "{ c.p.f: { c: ?0, n: ?1 } }", fields = "{ id: 1 }")
  def findUserIdByPhone(code: String, number: String): java.util.List[User]

  @Query(value = "{ id: { $in : ?0 } }", fields = "{ns: 0}")
  def findUsersByUserIds(userIds: Seq[ObjectId]): java.util.List[User]

  @Query(value = "{ id: { $in : ?0 } }", fields = "{ id: 1 }")
  def existedUserIds(userIds: Seq[ObjectId]): java.util.List[User]

  @Query(value = "{ id: ?0 }", fields = "{ um: 1 , ol: 1}")
  def getUserMedia(userId: ObjectId): User

  @Query(value = "{ id: ?0 }", fields = "{ st: 1 }")
  def getUserStatistics(userId: ObjectId): User

  @Query(value = "{ id: ?0 }", fields = "{ os: 1 }")
  def getUserOnlineStatus(userId: ObjectId): User
}
