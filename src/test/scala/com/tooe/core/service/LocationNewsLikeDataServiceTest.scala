package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.domain.{UserId, LocationId, LocationNewsId}
import com.tooe.core.db.mongo.domain.{LocationNewsLike, ObjectMap, LocationNews}
import java.util.Date
import com.tooe.api.service.{OffsetLimit, ChangeLocationNewsRequest}
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.util.Lang
import com.tooe.core.db.mongo.converters.MongoDaoHelper

class LocationNewsLikeDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationNewsLikeDataService = _
  lazy val entities = new MongoDaoHelper("location_news_likes")

  @Test
  def saveAndRead {
    val entity = new LocationNewsLikeFixture().like
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def remove {
    val entity = new LocationNewsLikeFixture().like
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.remove(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = new LocationNewsLikeFixture().like
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
      "lnid" : ${entity.locationNewsId.id.mongoRepr},
      "t" : ${entity.time.mongoRepr},
      "uid" : ${entity.userId.id.mongoRepr}
    }""")
  }

  @Test
  def getLikesByUserAndNews {
    val entity = new LocationNewsLikeFixture().like
    service.getLikesByUserAndNews(Seq(entity.locationNewsId), entity.userId) must haveSize(0)
    service.save(entity)
    val userLikes = service.getLikesByUserAndNews(Seq(entity.locationNewsId), entity.userId)
    userLikes must haveSize(1)
    userLikes must haveTheSameElementsAs(Seq(entity))
  }

  @Test
  def getLikes {
    val like1 = new LocationNewsLikeFixture().like
    val like2 = new LocationNewsLikeFixture().like
    service.save(like1)
    service.save(like2)
    service.getLikes(like1.locationNewsId, OffsetLimit(0, 10)) === Seq(like1)
    service.getLikes(like2.locationNewsId, OffsetLimit(0, 10)) === Seq(like2)
  }

  @Test
  def deleteLike {
    val like = new LocationNewsLikeFixture().like
    service.save(like)
    service.findOne(like.id) === Some(like)
    service.deleteLike(like.userId, like.locationNewsId)
    service.findOne(like.id) === None
  }

}

class LocationNewsLikeFixture {

  val like = LocationNewsLike(
    locationNewsId = LocationNewsId(),
    userId = UserId()
  )

}
