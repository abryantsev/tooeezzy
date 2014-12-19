package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.ModerationStatus
import com.tooe.core.domain.ModerationStatusId
import com.tooe.core.util.{Lang, HashHelper}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.mongodb.BasicDBObject

class ModerationStatusDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: ModerationStatusDataService = _

  lazy val entities = new MongoDaoHelper("moderation_status").collection


  @Test
  def saveAndReadAndDelete() {
    val entity = new ModerationStatusFixture().moderationStatus
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new ModerationStatusFixture().moderationStatus
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)(s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "ru" : "cool name" } ,
       "d" : { "ru" : "cool desc" }
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class ModerationStatusFixture {

  val moderationStatus = ModerationStatus(ModerationStatusId(HashHelper.uuid), Map(Lang.ru -> "cool name"), Map(Lang.ru -> "cool desc"))

}
