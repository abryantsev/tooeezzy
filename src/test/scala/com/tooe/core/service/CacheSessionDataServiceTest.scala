package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.{UserFixture, CacheSession}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.HashHelper
import com.tooe.core.domain.UserId
import org.bson.types.ObjectId
import com.tooe.core.util.DateHelper
import com.tooe.core.domain.SessionToken
import java.util.Date
import scala.util.Random

class CacheSessionDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: CacheSessionDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("cache_sessions")

  import HashHelper.uuid

  @Test
  def readWriteDelete() {
    val f = new CacheSessionFixture
    import f._
    service.find(token) === None
    service.save(cacheSession)
    service.find(token) !== None
    service.delete(token)
    service.find(token) === None
  }
  
  @Test
  def representation {
    val f = new CacheSessionFixture
    import f._
    service.save(cacheSession)
    val repr = entities.findOne(cacheSession.id.hash)
    jsonAssert(repr)(s"""{
      "_id" : "${cacheSession.id.hash}" ,
      "t" : ${cacheSession.createdAt.mongoRepr} ,
      "uid" : ${cacheSession.userId.id.mongoRepr}
    }""")
  }
}

class CacheSessionFixture {
  
  val user = new UserFixture().user

  def userId = user.id

  val token = SessionToken(HashHelper.uuid)
  
  val cacheSession = CacheSession(
    id = token,
    createdAt = new Date(),
    userId = userId
  )
}