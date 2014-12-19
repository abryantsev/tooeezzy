package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.domain.{NewsId, NewsCommentId, UserId}
import java.util.Date
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.NewsComment
import com.tooe.api.service.OffsetLimit

class NewsCommentDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: NewsCommentDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("news_comment")

  @Test
  def saveAndRead {
    val entity = (new NewsCommentFixture).newsComment
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = (new NewsCommentFixture).newsComment
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "pid" : ${entity.parentId.get.id.mongoRepr} ,
      "nid" : ${entity.newsId.id.mongoRepr} ,
      "t" : ${entity.creationDate.mongoRepr},
      "m" : "Good news, everyone",
      "aid" : ${entity.authorId.id.mongoRepr}
    }""")
  }

  @Test
  def updateMessage(){
    val entity = (new NewsCommentFixture).newsComment
    val newMessage = "new cool message"
    service.save(entity)
    service.updateMessage(entity.id, newMessage)
    service.findOne(entity.id).map(_.message) === Some(newMessage)
  }

  @Test
  def countComments(){
    val newsId = NewsId()
    val comments = (1 to 5).map(_ => service.save((new NewsCommentFixture).newsComment.copy(newsId = newsId))).toSeq

    service.countByNewsId(newsId) == comments.size
  }

  @Test
  def findComments(){
    val newsId = NewsId()
    val comments = (1 to 5).map(_ => service.save((new NewsCommentFixture).newsComment.copy(newsId = newsId))).toSeq

    val result = service.findByNewsId(newsId, OffsetLimit())
    result.size === comments.size
    result.zip(comments).foreach{
      case (f, e) => f === e
    }
  }
}

class NewsCommentFixture() {
  val authorId = UserId()
  val parentCommentId = Some(NewsCommentId(new ObjectId))
  val newsId = NewsId()
  val newsComment = NewsComment(
                                id = NewsCommentId(new ObjectId),
                                parentId = parentCommentId,
                                newsId = newsId,
                                creationDate = new Date,
                                message = "Good news, everyone",
                                authorId = authorId
                               )
}
