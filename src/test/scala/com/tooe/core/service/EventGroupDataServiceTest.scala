package com.tooe.core.service

import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.mongodb.BasicDBObject
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.domain.EventGroup
import com.tooe.core.domain.EventGroupId

class EventGroupDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: EventGroupDataService = _

  lazy val entities = new MongoDaoHelper("eventgroup").collection

  @Test
  def saveAndReadAndDelete() {
    val entity = new EventGroupFixture().eventGroup
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new EventGroupFixture().eventGroup
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "ru" : "cool name" }
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class EventGroupFixture {
  val eventGroup = EventGroup(EventGroupId.all,Map("ru" ->"cool name"))
}
