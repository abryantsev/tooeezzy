package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.domain.{ObjectMap, StarCategory}
import com.tooe.core.domain.{StarCategoryField, StarCategoryId}
import java.util.UUID
import com.tooe.core.util.Lang
import org.junit.Test

class StarsCategoriesDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: StarsCategoriesDataService = _

  implicit val lang = Lang.orig

  @Test
  def saveAndRead {
    val entity = new StarCategoryFixture().starCategory
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def find {
    val entity = new StarCategoryFixture().starCategory
    service.save(entity)
    val categories = service.find(Set())
    val firstCategory = categories.head
    firstCategory.id !== null
    firstCategory.name !== null
  }

  @Test
  def findByIds {
    val entity = new StarCategoryFixture().starCategory
    service.save(entity)
    val categories = service.findByIds(Seq(entity.id))
    val firstCategory = categories.head
    firstCategory.id === entity.id
    firstCategory.name === entity.name
  }

  @Test
  def findName {
    val entity = new StarCategoryFixture().starCategory
    service.save(entity)
    val categories = service.find(Set(StarCategoryField.Name, StarCategoryField.StarsCounter))
    val firstCategory = categories.head
    firstCategory.id !== null
    firstCategory.name !== null
    firstCategory.description === ObjectMap.empty
    firstCategory.categoryMedia === null
    firstCategory.starsCount === 0
  }

  @Test
  def updateSubscribers {
    val entity = new StarCategoryFixture().starCategory
    service.save(entity)

    service.updateSubscribers(entity.id, 2)
    service.findOne(entity.id).map(_.starsCount) === Some(2)

    service.updateSubscribers(entity.id, -1)
    service.findOne(entity.id).map(_.starsCount) === Some(1)
  }

}

class StarCategoryFixture {

  implicit val lang = Lang.orig

  val starCategory = StarCategory(
    id = StarCategoryId("star-id:" + UUID.randomUUID().toString),
    name = ObjectMap("star-name:" + UUID.randomUUID().toString),
    description = ObjectMap("star-description:" + UUID.randomUUID().toString),
    categoryMedia = "media-file.url",
    starsCount = 0,
    parentId = Some(StarCategoryId("star-id:" + UUID.randomUUID().toString))
  )

}