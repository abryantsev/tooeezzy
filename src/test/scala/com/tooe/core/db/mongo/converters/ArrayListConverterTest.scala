package com.tooe.core.db.mongo.converters

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.util.HashHelper
import org.junit.Test
import com.tooe.core.service.SpringDataMongoTestHelper
import com.tooe.core.db.mongo.domain.ArrayList
import com.tooe.core.db.mongo.domain.fromSeq
import com.tooe.core.infrastructure.AppContextTestHelper

class ArrayListConverterTest extends SpringDataMongoTestHelper {

  lazy val repo = AppContextTestHelper.lookupBean[ArrayListTestEntityRepository]

  lazy val entities = new MongoDaoHelper("ArrayListTestEntity")

  @Test
  def persistEmptyArrayList {
    val entity = ArrayListTestEntity(
    )
    repo.findOne(entity.id) must (beNull)
    repo.save(entity) === entity
    repo.findOne(entity.id) === entity

    val repr = entities.findOne(entity.id)
    println("repr="+repr)
    jsonAssert(repr)( s"""{
       "_id" : "${entity.id.toString}" ,
       "_class" : "com.tooe.core.db.mongo.converters.ArrayListTestEntity" ,
     }""")
  }

  @Test
  def persistNonEmptyArrayList {
    val entity = ArrayListTestEntity(
      field = Seq(ArrayListInnerTestEntity("first"), ArrayListInnerTestEntity("second"))
    )
    repo.findOne(entity.id) must (beNull)
    repo.save(entity) === entity
    repo.findOne(entity.id) === entity

    val repr = entities.findOne(entity.id)
    println("repr="+repr)
    jsonAssert(repr)( s"""{
       "_id" : "${entity.id.toString}" ,
       "_class" : "com.tooe.core.db.mongo.converters.ArrayListTestEntity" ,
       "field" : [ { field : "first" }, { field : "second" } ]
     }""")
  }
}

@Document(collection = "ArrayListTestEntity")
case class ArrayListTestEntity
(
  id: String = HashHelper.uuid,
  field: ArrayList[ArrayListInnerTestEntity] = ArrayList.empty
  )

case class ArrayListInnerTestEntity
(
  field: String
  )

trait ArrayListTestEntityRepository extends MongoRepository[ArrayListTestEntity, String]