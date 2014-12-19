package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.db.mongo.domain.EventType
import com.tooe.core.domain.{NewsTypeId, EventTypeId}
import com.tooe.core.util.HashHelper


class EventTypeDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: EventTypeDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("eventtype")

  @Test
  def saveAndRead {
    val entity = new EventTypeFixture().eventType
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new EventTypeFixture().eventType
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : "${entity.id.id}" ,
      "eg" : ["personal"],
      "n" : { "en" : "Friendship invitation" } ,
      "m" : { "en" : "was added" } ,
      "um" : { "en" : "has confirmed friendship" }
    }""")
  }

  @Test
  def getEventTypes {
    val entity = new EventTypeFixture().eventType
    service.save(entity)
    service.getEventTypes(Seq(entity.id)) === Seq(entity)
  }
}

class EventTypeFixture {
  val eventGroups  =  Seq("personal")
  val name = Map("en" -> "Friendship invitation")
  val message = Map("en" -> "was added")
  val userEventMessage = Map("en" -> "has confirmed friendship")
  val eventType = EventType(id = EventTypeId(HashHelper.str("eventTypeId")),
                            eventGroups = eventGroups,
                            name = name,
                            message = message,
                            userEventMessage = userEventMessage)
}

