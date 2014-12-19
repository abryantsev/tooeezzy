package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.{Query, MongoRepository}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.Product

trait ProductRepository extends MongoRepository[Product, ObjectId] {

  @Query(fields = "{ns: 0}")
  override def findOne(id: ObjectId): Product

  @Query(fields = "{ns: 0}")
  override def findAll(): java.util.List[Product]

  @Query(value = "{ rid: ?0 , pc: { $all: ?1}, ?2 : ?3 }", fields = "{ns: 0}")
  def searchProductsByRegion(regionId: ObjectId, category: Seq[String], locale: String, name: String): java.util.List[Product]

  @Query(value = "{ lid: ?0 , ?1 : ?2 }", fields = "{ns: 0}")
  def searchProductsByLocation(locationId: ObjectId, locale: String, name: String): java.util.List[Product]

  @Query(value = "{ id: { $in: ?0 }}", fields = "{ns: 0}")
  def getProducts(productIds: Seq[ObjectId]): java.util.List[Product]

  @Query(value = "{ id: { $in: ?0 }}", fields = "{ n: 1, pm: 1}")
  def getMiniWishItemProducts(productIds: Seq[ObjectId]): java.util.List[Product]
}
