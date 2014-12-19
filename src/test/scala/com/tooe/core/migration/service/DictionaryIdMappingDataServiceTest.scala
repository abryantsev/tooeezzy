package com.tooe.core.migration.service

import com.tooe.core.migration.infrastructure.MigrationSpringDataMongoTestHelper
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingDictionary._
import com.tooe.core.migration.db.domain.DictionaryIdMapping
import com.tooe.core.service.ObjectIdMongoReprHelper
import org.junit.Test

class DictionaryIdMappingDataServiceTest extends MigrationSpringDataMongoTestHelper {

  @Autowired var service: DictionaryIdMappingDataService = _

  lazy val entities = new MongoDaoHelper("idmapping_dic")

  def getEntity
  (id: ObjectId = new ObjectId(), dictionary: MappingDictionary = country, legacyId: Int = (System.currentTimeMillis() / 1000).toInt, newId: String = System.currentTimeMillis().toString) =
    DictionaryIdMapping(id, dictionary, legacyId, newId)
 /* @Test
  def saveAndRead {
    val entity = getEntity()
    service.find(entity.legacyId, entity.dictionary) === entity
  }

  @Test
  def representation {
    val entity = getEntity()
    val repr = entities.findOne(entity.id)
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.mongoRepr},
       "dn" : "${entity.dictionary.toString}",
       "lid" : ${entity.legacyId},
       "nid" : "${entity.newId}"
    }""")

  }*/

}
