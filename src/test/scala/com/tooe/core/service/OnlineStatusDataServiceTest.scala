package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.{ObjectMap, OnlineStatus}
import com.tooe.core.domain.OnlineStatusId
import com.mongodb.BasicDBObject
import com.tooe.core.util.HashHelper

class OnlineStatusDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: OnlineStatusDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("online_status")

  @Test
  def readWriteDelete() {
    val f = new OnlineStatusFixture
    import f._
    service.find(entity.id) === None
    service.save(entity)
    service.find(entity.id) !== None
    entities.collection.remove(new BasicDBObject("_id", entity.id.id))
  }
  
  @Test
  def representation {
    val f = new OnlineStatusFixture
    import f._
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : "${entity.id.id}" ,
      "n" : ${entity.name.mongoRepr}
    }""")
  }

  @Test
  def getStatusTest() {
    val f1, f2 = new OnlineStatusFixture
    service.save(f1.entity)
    service.save(f2.entity)
    service.getStatuses(f1.entity.id :: Nil) === Seq(f1.entity)
  }

  @Test
  def findAllTest() {
    val f = new OnlineStatusFixture
    service.save(f.entity)
    service.findAll must contain (f.entity)
  }
}

class OnlineStatusFixture {
  val entity = OnlineStatus(
    id = OnlineStatusId(HashHelper.uuid),
    name = ObjectMap(Map("orig" -> "online", "ru" -> "онлайн"))
  )
}

