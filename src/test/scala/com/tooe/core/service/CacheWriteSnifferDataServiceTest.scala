package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.CacheWriteSniffer
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.UserId
import java.util.Date

class CacheWriteSnifferDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: CacheWriteSnifferDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("cache_writesniffer")


  @Test
  def readWriteDelete() {
    val f = new CacheWriteSnifferFixture
    import f._
    service.find(entity.id) === None
    service.save(entity)
    service.find(entity.id) !== None
  }

  @Test
  def representation {
    val f = new CacheWriteSnifferFixture
    import f._
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "t" : ${entity.createdAt.mongoRepr} ,
      "uid" : ${entity.userId.id.mongoRepr}
    }""")
  }
}

class CacheWriteSnifferFixture {

  val entity = CacheWriteSniffer(
    userId = UserId(),
    createdAt = new Date()
  )
}
