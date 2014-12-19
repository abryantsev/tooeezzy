package com.tooe.core.migration.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.migration.db.domain.MappingCollection._
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.migration.infrastructure.MigrationSpringDataMongoTestHelper
import com.tooe.core.service.ObjectIdMongoReprHelper

class IdMappingDataServiceTest extends MigrationSpringDataMongoTestHelper {

  @Autowired var service: IdMappingDataService = _

  lazy val entities = new MongoDaoHelper("idmapping")

  def getEntity
  (id: ObjectId = new ObjectId(), collection: MappingCollection = user, legacyId: Int = (System.currentTimeMillis() / 1000).toInt, newId: ObjectId = new ObjectId()) =
    IdMapping(id, collection, legacyId, newId)


  @Test
  def saveAndRead {
    val entity = getEntity()
    /*try {
      service.find(entity.legacyId, entity.collection)
    } catch {
      case e: Exception =>
    }*/
    service.save(entity) === entity
    service.find(entity.legacyId, entity.collection) === entity
  }

  @Test
  def representation {
    val entity = getEntity()
    service.save(entity) === entity
    val repr = entities.findOne(entity.id)
    jsonAssert(repr)(s"""{
       "_id" : ${entity.id.mongoRepr},
       "cn" : "${entity.collection.toString}",
       "lid" : ${entity.legacyId},
       "nid" : ${entity.newId.mongoRepr}
    }""")

  }

}