package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.domain.WishLike
import com.tooe.core.domain.{UserId, WishId, WishLikeId}
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.exceptions.ApplicationException

trait WishLikeDataService {

  def save(entity: WishLike): WishLike

  def findOne(id: WishLikeId): Option[WishLike]

  def wishLikes(wishId: WishId, offsetLimit: OffsetLimit): Seq[WishLike]

  def delete(wishId: WishId, userId: UserId): WishLike

  def userLikeExists(wishId: WishId, userId: UserId): Boolean

  def likesQty(wishId: WishId): Long
}

@Service
class WishLikeDataServiceImpl extends WishLikeDataService {
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[WishLike]

  def save(entity: WishLike): WishLike = {
    mongo.save(entity)
    entity
  }

  def findOne(id: WishLikeId) = Option(mongo.findOne(new Query(new Criteria("_id").is(id.id)), classOf[WishLike]))

  def wishLikes(wishId: WishId, offsetLimit: OffsetLimit) = {
    val query = wishLikesQuery(wishId).withPaging(offsetLimit).desc("t")
    mongo.find(query, classOf[WishLike]).asScala
  }

  def likesQty(wishId: WishId): Long = mongo.count(wishLikesQuery(wishId), classOf[WishLike])

  private def wishLikesQuery(wishId: WishId) = new Query(new Criteria("wid").is(wishId.id))

  def delete(wishId: WishId, userId: UserId): WishLike = {
    val query = Query.query(new Criteria("wid").is(wishId.id).and("uid").is(userId.id))
    Option(mongo.findAndRemove(query, classOf[WishLike])).getOrElse({throw ApplicationException(message = "WishLike to delete was not found")})
  }

  def userLikeExists(wishId: WishId, userId: UserId) = {
    val query = Query.query(new Criteria("wid").is(wishId.id).and("uid").is(userId.id))
    mongo.findOne(query, classOf[WishLike]) != null
  }
}