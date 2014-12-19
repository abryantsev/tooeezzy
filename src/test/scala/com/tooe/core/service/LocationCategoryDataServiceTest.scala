package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.LocationCategory
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.CategoryField

class LocationCategoryDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationCategoryDataService = _

  lazy val entities = new MongoDaoHelper("location_category")

  @Test
  def saveAndRead {
    val entity = LocationCategory()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = LocationCategory(
      //TODO
    )
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : "${entity.id.id}" ,
      "n" : { } ,
      "cm" : []
    }""")
  }

  @Test
  def findAll {
    import CategoryField._
    val fields = Seq(Id, Name, Media)
    val entity = LocationCategory()
    service.findCategoriesBy(fields) must not contain (entity)
    service.save(entity)
    service.findCategoriesBy(fields).contains(entity) === true
    service.findCategoriesBy(fields).contains(entity) !== false
    service.findCategoriesBy(fields) must not contain (entity) //WTF? it shouldn't pass
    service.findCategoriesBy(fields) must contain (entity)
  }

  @Test
  def getLocationsCategories {
    val entity, entity2 = LocationCategory()
    service.save(entity)
    service.save(entity2)
    val result = service.getLocationsCategories(Seq(entity2.id))
    result must not contain (entity)
    result must contain (entity2)
  }
}