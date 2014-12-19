package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.InfoMessage
import com.tooe.core.util.HashHelper
import com.tooe.core.db.mongo.repository.InfoMessageRepository
import com.tooe.core.db.mongo.converters.MongoDaoHelper

class InfoMessageDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var repo: InfoMessageRepository = _
  @Autowired var service: InfoMessageDataServiceImpl = _

  lazy val entites = new MongoDaoHelper("infomessage")

  @Test
  def saveAndRead() {
    val entity = InfoMessage(
      id = HashHelper.uuid,
      message = Map("ru" -> HashHelper.uuid),
      errorCode = 100
    )
    repo.save(entity)

    jsonAssert(entites.findOne(entity.id))(s"""{
      "_id" : "${entity.id}" ,
      "_class" : "com.tooe.core.db.mongo.domain.InfoMessage" ,
      "m" : { "ru" : "${entity.message("ru")}"} ,
      "c" : 100
    }""")

    service.findOne(entity.id) === Some(entity)
  }
}