package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.db.mongo.domain.LocationStatistics
import java.util.Date
import com.tooe.core.domain.LocationId

class LocationStatisticsDataServiceTest extends SpringDataMongoTestHelper{

  @Autowired var service: LocationStatisticsDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("location_statistics")

  @Test
  def saveAndRead {
    val entity = new LocationStatisticsFixture().locationStatistics
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = new LocationStatisticsFixture().locationStatistics
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "lid" : ${entity.locationId.id.mongoRepr} ,
      "rt" : ${entity.registrationDate.mongoRepr},
      "vc" : 1 ,
    }""")
  }

}

class LocationStatisticsFixture {
  val locationStatistics = LocationStatistics(locationId = LocationId() ,
    registrationDate = new Date,
    visitorsCount = 1)
}
