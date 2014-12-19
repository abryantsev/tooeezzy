package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.WishRepository
import com.tooe.core.util.BuilderHelper._
import scala.collection.JavaConverters._
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import java.util.Date
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.domain.Wish
import UpdateResult.NoUpdate
import com.tooe.core.domain._
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.converters.DBObjectConverters
import com.tooe.api.service.OffsetLimit
import com.mongodb.BasicDBObject
import com.tooe.core.domain.UserId
import com.tooe.core.domain.ProductId
import com.tooe.core.domain.WishId
import scala.collection.convert.WrapAsScala

trait WishDataService {

  def find(userId: UserId, fulfilled: Boolean, offsetLimit: OffsetLimit): Seq[Wish]

  def updateReason(id: WishId, reasonText: Unsetable[String], reasonDate: Unsetable[Date]): UpdateResult

  def delete(id: WishId): DeleteResult

  def findByUserAndProduct(userId: UserId, productId: ProductId): Option[Wish]

  def findByUserAndLocation(userId: UserId, locationId: LocationId, fulfilledOnly: Boolean, offsetLimit: OffsetLimit): Seq[Wish]

  def countUserWishesPerLocation(userId: UserId, fulfilledOnly: Boolean, loc: LocationId): Int

  def fulfill(userId: UserId, productId: ProductId, fulfillmentDate: Date): Option[Wish]

  def updateUserLikes(wishId: WishId, userId: UserId): Unit

  def updateUserDislikes(wishId: WishId, userIds: Seq[UserId]): Unit

  def save(entity: Wish): Wish

  def findOne(id: WishId): Option[Wish]

  def findAll: Seq[Wish]

  def findWishes( ids: Set[WishId]): Seq[Wish]

  def markWishesAsDeleted(pid: ProductId): Unit
}

@Service
class WishDataServiceImpl extends WishDataService {
  @Autowired var repo: WishRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Wish]

  def find(userId: UserId, fulfilled: Boolean, offsetLimit: OffsetLimit): Seq[Wish] =
    repo.searchUsersWishes(userId.id, fulfilled, SkipLimitSort(offsetLimit).desc("t")).asScala

  def updateReason(id: WishId, reasonText: Unsetable[String], reasonDate: Unsetable[Date]): UpdateResult = {
    import DBObjectConverters._
    val update = new Update().setSkipUnset("r", reasonText).setSkipUnset("rd", reasonDate)
    if (update.getUpdateObject.keySet().isEmpty) NoUpdate
    else mongo.updateFirst(
      Query.query(new Criteria("_id").is(id.id)),
      update,
      entityClass
    ).asUpdateResult
  }

  def delete(id: WishId) = {
    repo.delete(id.id)
    DeleteResult.Deleted
  }

  def findByUserAndProduct(userId: UserId, productId: ProductId) =
    Option(repo.findWishByProduct(userId.id, productId.id))

  def countUserWishesPerLocation(userId: UserId, fulfilledOnly: Boolean, loc: LocationId) = {
    val query = Query.query(Criteria.where("p.lid").is(loc.id).and("uid").is(userId.id)
      .extend(fulfilledOnly){_.and("ft").exists(true)})
    mongo.count(query, entityClass).toInt
  }

  def fulfill(userId: UserId, productId: ProductId, fulfillmentDate: Date) = {
    val query = Query.query(new Criteria("uid").is(userId.id).and("p.pid").is(productId.id).and("ft").exists(false)).asc("t")
    val update = new Update().set("ft", fulfillmentDate)
    Option(mongo.findAndModify(query, update, entityClass))
  }

  def updateUserLikes(id: WishId, userId: UserId) = {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("lc", 1).push("ls", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", -20))
    mongo.updateFirst(query, update, entityClass)
  }

  def updateUserDislikes(id: WishId, userIds: Seq[UserId]) = {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("lc", -1).set("ls", userIds.map(_.id))
    mongo.updateFirst(query, update, entityClass)
  }

  def save(entity: Wish) = repo.save(entity)

  def findOne(id: WishId) = Option(repo.findOne(id.id))

  def findAll = repo.findAll.asScala


  def findWishes(ids: Set[WishId]): Seq[Wish] = {
    mongo.find(Query.query(Criteria.where("id").in(ids.map(_.id).asJavaCollection)),
      entityClass).asScala.toSeq
  }

  def markWishesAsDeleted(pid: ProductId) {
    val query = Query.query(new Criteria("p.pid").is(pid.id))
    val update = new Update().set("lfs", "r")
    mongo.updateMulti(query, update, entityClass)
  }
  def findByUserAndLocation(userId: UserId, locationId: LocationId, fulfilledOnly: Boolean, offsetLimit: OffsetLimit): Seq[Wish] = {
    val query = Query.query(Criteria.where("p.lid").is(locationId.id).and("uid").is(userId.id)
      .extend(fulfilledOnly){_.and("ft").exists(true)}).withPaging(offsetLimit)
    WrapAsScala.asScalaBuffer(mongo.find(query, entityClass))
  }
}