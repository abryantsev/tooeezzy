package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.domain.LifecycleStatus
import com.tooe.core.domain.LifecycleStatusId
import com.tooe.core.util.{Lang, HashHelper}

class LifecycleStatusDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: LifecycleStatusDataService = _

  lazy val entities = new MongoDaoHelper("lifecycle_status").collection

  @Test
  def saveAndReadAndDelete() {
    val entity = new LifecycleStatusFixture().lifecycleStatus
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new LifecycleStatusFixture().lifecycleStatus
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "ru" : "крутой период" }
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class LifecycleStatusFixture{
  val lifecycleStatus = LifecycleStatus(LifecycleStatusId(HashHelper.uuid), Map(Lang.ru -> "крутой период"))
}
