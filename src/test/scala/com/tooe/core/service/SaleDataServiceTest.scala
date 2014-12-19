package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.{LocationShort, ProductShort, Sale}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.bson.types.ObjectId
import java.util.Date

class SaleDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: SaleDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("sale")

  @Test
  def saveAndRead {
    val entity = (new SaleFixture).sale
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = (new SaleFixture).sale
    service.save(entity)
    val repr = entities.findOne(entity.id)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.toString}" } ,
      "_class" : "com.tooe.core.db.mongo.domain.Sale",
      "p" :  {
          "pid" : { "$$oid" : "${entity.product.productId.toString}" },
          "n" : {
            "orig" : "${entity.product.name("orig")}"
          }
       },
      "st" : ${entity.startDate.mongoRepr},
      "et" : ${entity.endDate.mongoRepr},
      "lo" : {
          "lid" : { "$$oid" : "${entity.location.locationId.toString}" },
          "rid" : { "$$oid" : "${entity.location.regionId.toString}" }
      }
    }""")
  }
}

class SaleFixture {

  val sale = Sale(
    product = ProductShort(productId = new ObjectId, name = Map("orig" -> "name")),
    location = LocationShort(locationId = new ObjectId, regionId = new ObjectId),
    startDate = new Date,
    endDate = new Date
  )

}