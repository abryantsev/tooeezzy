package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import com.tooe.core.db.mongo.domain.Checkin
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable

trait CheckinRepository  extends MongoRepository[Checkin, ObjectId] {

  @Query("{ u.uid: ?0 }")
  def searchUserCheckin(userId: ObjectId): Checkin

  @Query(value = CheckinRepository.nearSphereQuery, fields = "{ fs : 0 }")
  def nearSphere(lon: Double, lat: Double, maxDistance: Double, excludeUserId: ObjectId, pageable: Pageable): java.util.List[Checkin]

  @Query(value = CheckinRepository.nearSphereQuery, count = true)
  def nearSphereCount(lon: Double, lat: Double, maxDistance: Double, excludeUserId: ObjectId): Long
}

object CheckinRepository {

  final val nearSphere =
    """{ $nearSphere : { $geometry : { type : "Point" , coordinates : [ ?0 , ?1 ] } } , $maxDistance : ?2 }"""

  final val nearSphereQuery = "{ lo.l : "+nearSphere+" , u.uid : { $ne : ?3 } }"
}