package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import org.springframework.data.domain.Pageable
import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.domain.Location

trait LocationRepository  extends MongoRepository[Location, ObjectId] {
  import LocationRepository._

  @Query(fields = "{ns: 0}")
  override def findAll(): java.util.List[Location]

  @Query(fields = "{ns: 0}")
  override def findOne(id: ObjectId): Location

  @Query(value = GeoWithinQuery, fields = "{ns: 0}")
  def searchGeoWithin(longitude: Double, latitude: Double, radius: Double): java.util.List[Location]

  @Query(value = GeoWithinQuery, count = true)
  def searchGeoWithinCount(longitude: Double, latitude: Double, radius: Double): Int

  @Query(value = NearSphereQuery, fields = "{ns: 0}")
  def searchNearSphere(longitude: Double, latitude: Double, radius: Int): java.util.List[Location]

  @Query(value = GeoWithinWithCategoryFilterQuery, fields = "?4")
  def findLocationsGeoWithin(longitude: Double,
                               latitude: Double,
                               radius: Double,
                               categoryId:String, fields: BasicDBObject, pageable: Pageable): java.util.List[Location]

  @Query(value = GeoWithinWithCategoryFilterQuery, count = true)
  def findLocationsGeoWithinCount(longitude: Double,
                               latitude: Double,
                               radius: Double,
                               categoryId:String): Int

  @Query(value = "{ id: { $in: ?0}}", fields = "{ns: 0}")
  def getLocations(locationIds:Seq[ObjectId]): java.util.List[Location]

}

object LocationRepository {
  final val NearSphereQuery= """{c.a.l: { $nearSphere : { $geometry : { type: "Point", coordinates: [ ?0, ?1 ] } , $maxDistance: ?2 }}}"""

  final val GeoWithinQuery= "{c.a.l: { $geoWithin : { $centerSphere : [ [ ?0, ?1 ] , ?2 ]}}}"

  final val GeoWithinWithCategoryFilterQuery = "{c.a.l: { $geoWithin : { $centerSphere : [ [ ?0, ?1 ] , ?2 ]}}, lc : ?3, lfs: { $exists: false }}"
}
