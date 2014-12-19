package com.tooe.core.db.mongo.converters

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.util.HashHelper
import org.junit.Test
import com.tooe.core.service.SpringDataMongoTestHelper
import org.springframework.beans.factory.annotation.Autowired

class SeqConverterTest extends SpringDataMongoTestHelper {

  @Autowired var repo: SeqTestEntityRepository = _

  lazy val entities = new MongoDaoHelper("SeqTestEntity")

  @Test
  def persistEmptySeq {
    val entity = SeqTestEntity(
    )
    repo.findOne(entity.id) must (beNull)
    repo.save(entity) === entity
    repo.findOne(entity.id) === entity

    val repr = entities.findOne(entity.id)
    println("repr="+repr)
    jsonAssert(repr)( s"""{
       "_id" : "${entity.id.toString}" ,
       "_class" : "com.tooe.core.db.mongo.converters.SeqTestEntity" ,
     }""")
  }

  @Test
  def persistNonEmptySeq {
    val entity = SeqTestEntity(
      field = Seq("first", "second")
    )
    repo.findOne(entity.id) must (beNull)
    repo.save(entity) === entity
    repo.findOne(entity.id) === entity

    val repr = entities.findOne(entity.id)
    println("repr="+repr)
    jsonAssert(repr)( s"""{
       "_id" : "${entity.id.toString}" ,
       "_class" : "com.tooe.core.db.mongo.converters.SeqTestEntity" ,
       "field" : [ "first" , "second"]
     }""")
  }
}

@Document(collection = "SeqTestEntity")
case class SeqTestEntity
(
  id: String = HashHelper.uuid,
  field: Seq[String] = Nil
  )

trait SeqTestEntityRepository extends MongoRepository[SeqTestEntity, String]