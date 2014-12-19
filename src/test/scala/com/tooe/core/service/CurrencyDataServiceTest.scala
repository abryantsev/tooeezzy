package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.domain.Currency
import com.tooe.core.domain.CurrencyId
import com.tooe.core.util.{Lang, HashHelper}
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.mongodb.BasicDBObject

class CurrencyDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: CurrencyDataService = _

  lazy val entities = new MongoDaoHelper("currency").collection

  @Test
  def saveAndReadAndDelete() {
    val entity = new CurrencyFixture().currency
    service.findAll.contains(entity) === false
    service.save(entity)
    service.findAll.contains(entity) === true
    entities.remove(new BasicDBObject("_id", entity.id.id))
    service.findAll.contains(entity) === false
  }

  @Test
  def representation {
    val entity = new CurrencyFixture().currency
    service.save(entity)
    val repr = entities.findOne(new BasicDBObject("_id", entity.id.id))
    jsonAssert(repr)(s"""{
       "_id" : ${entity.id.id} ,
       "c" :  ${entity.curs} ,
       "n" : { "ru" : "тугрик" } ,
       "nc" :  ${entity.numcode}
    }""")
    entities.remove(new BasicDBObject("_id", entity.id.id))
  }

}

class CurrencyFixture {
  val currency = Currency(CurrencyId(HashHelper.uuid), Map(Lang.ru -> "тугрик"), 2.0, 100)
}