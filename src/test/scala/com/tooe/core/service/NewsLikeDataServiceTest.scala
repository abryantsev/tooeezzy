package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.domain.{NewsId, UserId}
import java.util.Date
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.{NewsLikeId, NewsLike}
import com.tooe.api.service.OffsetLimit

class NewsLikeDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: NewsLikeDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("news_like")

  @Test
  def saveAndRead() {
    val entity = (new NewsLikeFixture).newsLike
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation() {
    val entity = (new NewsLikeFixture).newsLike
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "nid" : ${entity.newsId.id.mongoRepr} ,
      "t" : ${entity.time.mongoRepr},
      "uid" : ${entity.userId.id.mongoRepr}
    }""")
  }

  @Test
  def findAllByNewsId() {
    val newsId = NewsId()
    val newsLikes = (1 to 5).map(_ => service.save(new NewsLikeFixture().newsLike.copy(newsId = newsId)))
    val result = service.findAllByNewsId(newsId, OffsetLimit())
    result.size === newsLikes.size
    result.zip(newsLikes).foreach {
      case (f, e) => f === e
    }
  }

  @Test
  def countByNewsId() {
    val newsId = NewsId()
    val newsLikes = (1 to 5).map(_ => service.save(new NewsLikeFixture().newsLike.copy(newsId = newsId)))
    val result = service.countByNewsId(newsId)
    result === newsLikes.size
  }
}

class NewsLikeFixture() {
  val userId = UserId()
  val newsId = NewsId()
  val newsLike = NewsLike(
    id = NewsLikeId(new ObjectId),
    newsId = newsId,
    time = new Date,
    userId = userId)
}

