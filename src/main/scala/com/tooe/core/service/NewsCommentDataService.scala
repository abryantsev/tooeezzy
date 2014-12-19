package com.tooe.core.service

import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.domain.NewsComment
import com.tooe.core.domain.{NewsId, NewsCommentId}
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.NewsCommentRepository
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.query.{PagingHelper, SortHelper, WriteResultHelper, UpdateResult}
import org.springframework.data.mongodb.core.query.{Query, Update, Criteria}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.exceptions.ApplicationException

trait NewsCommentDataService {
  def save(entity: NewsComment): NewsComment
  def findOne(id: NewsCommentId): Option[NewsComment]
  def delete(id: NewsCommentId): NewsComment
  def updateMessage(id: NewsCommentId, message: String): UpdateResult
  def countByNewsId(newsId: NewsId): Long
  def findByNewsId(newsId: NewsId, offsetLimit: OffsetLimit): Seq[NewsComment]
}

@Service
class NewsCommentDataServiceImpl extends NewsCommentDataService {
  import scala.collection.JavaConversions._

  @Autowired var repo: NewsCommentRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[NewsComment]

  def save(entity: NewsComment): NewsComment = repo.save(entity)

  def findOne(id: NewsCommentId): Option[NewsComment] = Option(repo.findOne(id.id))

  def delete(id: NewsCommentId): NewsComment = {
    val query = Query.query(new Criteria("_id").is(id.id))
    Option(mongo.findAndRemove(query, entityClass)).getOrElse(throw ApplicationException(message = "NewsComment wasn't found to delete"))
  }

  def updateMessage(id: NewsCommentId, message: String) = {
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().set("m", message)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def countByNewsId(newsId: NewsId) = mongo.count(Query.query(Criteria.where("nid").is(newsId.id)), entityClass)

  def findByNewsId(newsId: NewsId, offsetLimit: OffsetLimit) =
    mongo.find(Query.query(Criteria.where("nid").is(newsId.id)).desc("t").withPaging(offsetLimit).asc("t"), entityClass)

}
