package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.domain.{LocationsChainId, UserId, LocationId, LocationNewsId}
import com.tooe.core.db.mongo.domain.{ObjectMap, LocationNews}
import java.util.Date
import com.tooe.api.service.{OffsetLimit, ChangeLocationNewsRequest}
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.util.Lang
import com.tooe.core.db.mongo.converters.MongoDaoHelper

class LocationNewsDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationNewsDataService = _
  lazy val entities = new MongoDaoHelper("location_news")

  def getEntity(id: LocationNewsId = LocationNewsId(), content: ObjectMap[String] = ObjectMap.empty, commentEnable: Option[Boolean] = None, createdTime: Date = new Date, locationId: LocationId = LocationId(), locationsChainId: Option[LocationsChainId] = Some(LocationsChainId())) =
    LocationNews(id, locationId, content, commentEnable, createdTime, 0, Nil, locationsChainId)

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def remove {
    val entity = getEntity()
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.remove(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def update {

    implicit val lang = Lang.ru

    val entity = getEntity(content = Map("orig" -> "original news", "ru" -> "russian news"))
    service.save(entity)

    val newNews = Some("new russian news")

    service.update(entity.id, ChangeLocationNewsRequest(newNews, Update(true)), lang)

    val updatedNews = service.findOne(entity.id)

    updatedNews.flatMap(_.content.localized) === newNews
    updatedNews.flatMap(_.content.localized(Lang.orig)) === entity.content.localized(Lang.orig)
    updatedNews.flatMap(_.commentsEnabled) === Some(true)
  }

  @Test
  def getLocationNews {
    val locationId = LocationId()
    val entities = (1 to 3).map(i => getEntity(locationId = locationId))
    entities.foreach(service.save)

    (1 to 3).map(i => getEntity()).foreach(service.save)

    val locationsNews = service.getLocationNews(locationId, OffsetLimit())
    locationsNews must haveTheSameElementsAs(entities)
    locationsNews.size === 3

  }

  @Test
  def getLocationNewsCount {
    val locationId = LocationId()
    val entities = (1 to 3).map(i => getEntity(locationId = locationId))
    entities.foreach(service.save)

    (1 to 3).map(i => getEntity()).foreach(service.save)

    service.getLocationNewsCount(locationId) === 3
  }

  @Test
  def getLocationsChainNews {
    val locationsChainId = LocationsChainId()
    val entities = (1 to 3).map(i => getEntity(locationsChainId = Some(locationsChainId)))
    entities.foreach(service.save)

    (1 to 3).map(i => getEntity()).foreach(service.save)

    val locationsNews = service.getLocationsChainNews(locationsChainId, OffsetLimit())
    locationsNews must haveTheSameElementsAs(entities)
    locationsNews.size === 3

  }

  @Test
  def getLocationsChainNewsCount {
    val locationsChainId = LocationsChainId()
    val entities = (1 to 3).map(i => getEntity(locationsChainId = Some(locationsChainId)))
    entities.foreach(service.save)

    (1 to 3).map(i => getEntity()).foreach(service.save)

    service.getLocationsChainNewsCount(locationsChainId) === 3
  }

  @Test
  def representation {
    val entity = getEntity()
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
      "lid" : ${entity.locationId.id.mongoRepr},
      "t" : ${entity.createdTime.mongoRepr},
      "c" : { },
      "lc" : 0,
      "ls" : [],
      "lcid" : ${entity.locationsChainId.get.id.mongoRepr}
    }""")
  }

  @Test
  def updateUserLikes {
    val entity = getEntity()
    service.save(entity)
    val fakeUserIds = (1 to 20).map(_ => UserId())
    fakeUserIds.foreach { userId =>
      service.updateUserLikes(entity.id, userId)
      val currentEntity = service.findOne(entity.id).get
      currentEntity.lastLikes must contain(userId)
    }
    val currentEntity = service.findOne(entity.id).get
    currentEntity.likesCount === 20
    currentEntity.lastLikes must haveSize(10)
    currentEntity.lastLikes must haveTheSameElementsAs(fakeUserIds.drop(10).toSeq)
  }

  @Test
  def updateUserUnlikes {
    val entity = getEntity()
    service.save(entity)
    val fakeUserIds = (1 to 20).map(_ => UserId()).toList
    fakeUserIds.foreach { userId =>
      service.updateUserLikes(entity.id, userId)
    }

    val userStillLikes = fakeUserIds.drop(10).take(10)
    service.updateUserUnlikes(entity.id, userStillLikes)

    val currentEntity = service.findOne(entity.id).get
    currentEntity.lastLikes must haveTheSameElementsAs(userStillLikes)
  }

}

