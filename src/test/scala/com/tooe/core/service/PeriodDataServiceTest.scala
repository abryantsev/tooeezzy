package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.domain.Period
import com.tooe.core.domain.PeriodId
import com.tooe.core.util.{Lang, HashHelper}

class PeriodDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: PeriodDataService = _

  lazy val entities = new MongoDaoHelper("period").collection

  @Test
  def saveAndReadAndDelete() {
    val entity = new PeriodFixture().period
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new PeriodFixture().period
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.id} ,
       "n" : { "ru" : "крутой период" }
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class PeriodFixture {
  val period = Period(PeriodId(HashHelper.uuid), Map(Lang.ru -> "крутой период"))
}
