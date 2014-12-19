package com.tooe.core.service

import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.domain.{NewsLikeId, NewsLike}
import com.tooe.core.domain.{UserId, NewsId}
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.NewsLikeRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query.PagingHelper

sealed trait NewsLikeDataService {
  def save(entity: NewsLike): NewsLike

  def remove(newsId: NewsId, userId: UserId): NewsLike

  def findOne(id: NewsLikeId): Option[NewsLike]

  def findAllByNewsId(newsId: NewsId, offsetLimit: OffsetLimit): Seq[NewsLike]

  def countByNewsId(newsId: NewsId): Long
}

@Service
class NewsLikeDataServiceImpl extends NewsLikeDataService {

  import scala.collection.JavaConversions._

  @Autowired var repo: NewsLikeRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[NewsLike]

  def save(entity: NewsLike): NewsLike = repo.save(entity)

  def remove(newsId: NewsId, userId: UserId): NewsLike =
    mongo.findAndRemove(Query.query(Criteria.where("nid").is(newsId.id).and("uid").is(userId.id)), entityClass)

  def findOne(id: NewsLikeId): Option[NewsLike] = Option(repo.findOne(id.id))

  def findAllByNewsId(newsId: NewsId, offsetLimit: OffsetLimit) = mongo.find(byNewsIdQuery(newsId).withPaging(offsetLimit), entityClass)

  def countByNewsId(newsId: NewsId) = mongo.count(byNewsIdQuery(newsId), entityClass)

  private def byNewsIdQuery(newsId: NewsId) = Query.query(Criteria.where("nid").is(newsId.id))
}


