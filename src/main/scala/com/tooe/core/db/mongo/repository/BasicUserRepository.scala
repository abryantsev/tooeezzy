package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.BasicUser
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.Query

trait BasicUserRepository  extends MongoRepository[BasicUser, ObjectId]{
  def findByUname(uname:String):java.util.List[BasicUser]

  @Query("{ 'uname' : ?0 }")
  def findBasicUsersWithName(uname:String):java.util.List[BasicUser]

  @Query(value="{ 'age' : ?0 }")
  def findBasicUsersWithAge(age:Int):java.util.List[BasicUser]

  @Query(value="{ $and: [{ 'uname' : ?0},{ 'age' :  { $gt:?1, $lt:?2 } }]}")
  def findBasicUsersWithArgs(uname:String, ageMin:Int, ageMax:Int):java.util.List[BasicUser]
}
