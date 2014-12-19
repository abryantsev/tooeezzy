package com.tooe.core.service

import com.mongodb.BasicDBObject
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.converters.{DBObjectConverter, NewsConverter}
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.repository.NewsRepository
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query._
import org.springframework.stereotype.Service
import com.tooe.core.usecase.news.NewsType
import org.springframework.data.domain.Sort
import scala.collection.JavaConverters._

trait NewsDataService {
  def save(entity: News): News
  def find(id: NewsId): Option[News]
  def delete(id: NewsId): Unit
  def findAllNews(userId: UserId, newsType: NewsType, offsetLimit: OffsetLimit): Seq[News]
  def findAllNewsForUser(userId: UserId, offsetLimit: OffsetLimit): Seq[News]
  def findLastNewsByTypeForActor(userId: UserId, newsTypeId: NewsTypeId): Option[News]
  def updateUserCommentMessage(id: NewsId, message: String): UpdateResult
  def deleteUserComment(id: NewsId): Unit
  def updateAddComment(id: NewsId, comment: NewsCommentShort): UpdateResult
  def updatePhotoAlbumPhotosAndPhotosCounter(id: NewsId, photoId: PhotoId): UpdateResult
  def updateDeleteComment(id: NewsId, newsCommentId: NewsCommentId): UpdateResult
  def updateNewsCommentMessage(id: NewsId, newsCommentId: NewsCommentId, message: String): UpdateResult
  def updateUserLikes(id: NewsId, userId: UserId): UpdateResult
  def updateUserUnlikes(id: NewsId, userId: UserId): UpdateResult
  def updateUserHideNews(id: NewsId, userId: UserId, viewerType: NewsViewerType): UpdateResult
  def updateUserRestoreNews(id: NewsId, userId: UserId, viewerType: NewsViewerType): UpdateResult
}

@Service
class NewsDataServiceImpl extends NewsDataService {
  @Autowired var repo: NewsRepository = _
  @Autowired var mongo: MongoTemplate = _

  private val converter = new Object with NewsConverter


  val entityClass = classOf[News]

  def save(entity: News): News = repo.save(entity)

  def find(id: NewsId) = Option(repo.findOne(id.id))

  def delete(id: NewsId) = repo.delete(id.id)

  def findAllNews(userId: UserId, newsType: NewsType, offsetLimit: OffsetLimit) = {
    val criteria = newsType match {
      case NewsType.All =>
        new Criteria().orOperator(Criteria.where("uids").is(userId.id).and("hus").ne(userId.id), Criteria.where("nt")
          .is(NewsTypeId.LocationTooeezzyNews.id))
      case NewsType.My =>
        Criteria.where("arid").is(userId.id).and("har").ne(userId.id)
    }
    mongo.find(Query.query(criteria).withPaging(offsetLimit).desc("t"), entityClass).asScala.toSeq
  }

  def findAllNewsForUser(userId: UserId, offsetLimit: OffsetLimit) = {
    mongo.find(Query.query(Criteria.where("arid").is(userId.id).and("har").ne(userId.id))
      .withPaging(offsetLimit).desc("t"), entityClass).asScala.toSeq
  }

  def findLastNewsByTypeForActor(userId: UserId, newsTypeId: NewsTypeId) = {
    val query = Query.query(Criteria.where("aid").is(userId.id).and("nt").is(newsTypeId.id)).sort(Sort.Direction.DESC, "_id")
    Option(mongo.findOne(query, entityClass))
  }

  def updateUserCommentMessage(id: NewsId, message: String ): UpdateResult =
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(id.id)),
      (new Update).set("uc.m", message),
      classOf[News]
    ).asUpdateResult

  def deleteUserComment(userCommentId: NewsId): Unit =
    repo.delete(userCommentId.id)

  def updateAddComment(id: NewsId, comment: NewsCommentShort) = {
    import converter._
    val obj = implicitly[DBObjectConverter[NewsCommentShort]].serialize(comment)
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = (new Update).inc("cc", 1).push("cs", new BasicDBObject("$each", java.util.Arrays.asList(obj)).append("$slice", -3) )
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateDeleteComment(id: NewsId, newsCommentId: NewsCommentId) = {
    val query = Query.query(new Criteria("_id").is(id.id).and("cs._id").is(newsCommentId.id))
    val update = (new Update).inc("cc", -1).pull("cs", new BasicDBObject("_id", newsCommentId.id))
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateNewsCommentMessage(id: NewsId, newsCommentId: NewsCommentId, message: String) = {
    val query = Query.query(new Criteria("_id").is(id.id).and("cs._id").is(newsCommentId.id))
    val update = (new Update).set("cs.$.m", message)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateUserLikes(id: NewsId, userId: UserId) = {
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = (new Update).inc("lc", 1).push("ls", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", -10) )
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updatePhotoAlbumPhotosAndPhotosCounter(id: NewsId, photoId: PhotoId) = {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("pa.pc", 1).push("pa.ps", new BasicDBObject("$each", java.util.Arrays.asList(photoId.id)).append("$slice", -10) )
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateUserUnlikes(id: NewsId, userId: UserId) = {
    val query = Query.query(new Criteria("_id").is(id.id).and("ls").is(userId.id))
    val update = (new Update).inc("lc", -1).pull("ls", userId.id)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def updateUserHideNews(id: NewsId, userId: UserId, viewerType: NewsViewerType) = {
    val (from, to) = viewerType match {
      case NewsViewerType.Viewer => ("uids", "hus")
      case _ => ("arid", "har")
    }
    hideOrRestoreNews(id, userId, from, to)
  }

  def updateUserRestoreNews(id: NewsId, userId: UserId, viewerType: NewsViewerType) = {
    val (from, to) = viewerType match {
      case NewsViewerType.Viewer => ("hus", "uids")
      case _ => ("har", "arid")
    }
    hideOrRestoreNews(id, userId, from, to)
  }

  private def hideOrRestoreNews(id: NewsId, userId: UserId, from: String, to: String): UpdateResult = {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = new Update().pull(from, userId.id).push(to, userId.id)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }
}