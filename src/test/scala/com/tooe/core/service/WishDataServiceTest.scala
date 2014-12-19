package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import java.util.Date
import com.tooe.core.util.DateHelper
import com.tooe.core.domain.{LocationId, WishLifecycleId, UserId, ProductId}
import com.tooe.core.util.TestDateGenerator
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.domain.Unsetable._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.domain.ProductRef
import com.tooe.core.db.mongo.domain.Wish
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.domain.Unsetable.Update
import scala.collection.convert.WrapAsScala

class WishDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: WishDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("wish")

  @Test
  def saveAndRead {
    val entity = (new WishFixture).wish
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def findAll {
    val entity = (new WishFixture).wish
    service.findAll must not contain (entity)
    service.save(entity)
    service.findAll.contains(entity) must beTrue
  }

  @Test
  def representation {
    val entity = (new WishFixture).wish
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr},
      "lc" : 0 ,
      "lfs" : "r" ,
      "t" : ${entity.creationDate.mongoRepr},
      "ls": [],
      "p" : {
        "lid": ${entity.product.locationId.id.mongoRepr},
        "pid": ${entity.product.productId.id.mongoRepr}
       },
       "uid" : ${entity.userId.id.mongoRepr}
    }""")
  }

  @Test
  def findByProduct {
    val f = new WishFixture
    import f._
    service.findByUserAndProduct(userId, productId) === None
    service.save(wish)
    service.findByUserAndProduct(userId, productId) === Some(wish)
  }

  @Test
  def findByUserId {
    val ourWish = (new WishFixture).wish
    val ourWishId = ourWish.userId
    val anotherWish = (new WishFixture).wish
    service.find(userId = ourWishId, offsetLimit = OffsetLimit(0, 1), fulfilled = false) must (beEmpty)
    service.save(ourWish)
    service.save(anotherWish)
    service.find(userId = ourWishId, offsetLimit = OffsetLimit(0, 1), fulfilled = false) === Seq(ourWish)
  }

  @Test
  def findByFulfillment {
    val userId = UserId()
    val wish1 = (new WishFixture(userId, Some(DateHelper.currentDate))).wish
    val wish2 = (new WishFixture(userId)).wish
    service.save(wish1)
    service.save(wish2)
    service.find(userId = userId, fulfilled = true, OffsetLimit(0, 1)) === Seq(wish1)
    service.find(userId = userId, fulfilled = false, OffsetLimit(0, 1)) === Seq(wish2)
    service.find(userId = userId, fulfilled = true, OffsetLimit(1, 1)) === Seq()
  }

  @Test
  def updateReason {
    val wish = (new WishFixture).wish
    service.updateReason(wish.id, reasonText = Skip, reasonDate = Update(new Date)) === UpdateResult.NotFound

    service.save(wish)
    service.updateReason(wish.id, reasonText = Update("some text"), reasonDate = Skip) === UpdateResult.Updated
    service.findOne(wish.id).get.reason === Some("some text")

    val newDate = new Date
    service.updateReason(wish.id, reasonText = Unset, reasonDate = Update(newDate)) === UpdateResult.Updated
    service.findOne(wish.id).get.reason === None
    service.findOne(wish.id).get.reasonDate.get === newDate
  }

  @Test
  def deleteReason {
    val wish = (new WishFixture).wish
    service.save(wish)
    service.delete(wish.id)
    service.findOne(wish.id) === None
  }

  @Test
  def fulfillWish {
    val f = new FulfillWishFixture {
      val wish = createWish()
    }
    import f._

    service.fulfill(userId, productId, fulfillmentDate) === None
    service.save(wish)
    service.fulfill(userId, productId, fulfillmentDate) === Some(wish)

    val found = service.findOne(wish.id).orNull
    found !== null
    found.fulfillmentDate === Some(fulfillmentDate)
  }

  @Test
  def fulfillEarlyWishFirst {
    val f = new FulfillWishFixture {
      val tdg = new TestDateGenerator()
      val wish1, wish2 = createWish(tdg.next())
    }
    import f._

    service.save(wish1)
    service.save(wish2)

    service.fulfill(userId, productId, fulfillmentDate)

    service.findOne(wish1.id).get.fulfillmentDate === Some(fulfillmentDate)
    service.findOne(wish2.id).get.fulfillmentDate === None
  }

  @Test
  def dontFulfillFulfilledWith {
    val f = new FulfillWishFixture {
      val fulfilledWish = createWish().copy(fulfillmentDate = Some(DateHelper.currentDate))
    }
    import f._

    service.save(fulfilledWish)
    service.fulfill(userId, productId, fulfillmentDate) === None
  }

  @Test
  def updateUserLikes {
    val entity = new WishFixture().wish
    service.save(entity)
    val firstLikerId = UserId(new ObjectId())
    service.updateUserLikes(entity.id, firstLikerId)

    val twentyLastLikerIds = (1 to 20) map (_ => UserId())
    twentyLastLikerIds.foreach {
      service.updateUserLikes(entity.id, _)
    }

    val likedEntity = service.findOne(entity.id).get
    likedEntity.likesCount === 21
    likedEntity.usersWhoSetLikes === twentyLastLikerIds
  }

  @Test
  def updateUserDislikes {

    val likers = (1 to 20) map (_ => UserId())
    val entity = new WishFixture(likesCount = 21, usersWhoSetLikes = likers).wish
    service.save(entity)
    service.updateUserDislikes(entity.id, likers.take(20))
    val e = service.findOne(entity.id)
    val updatedEntity = service.findOne(entity.id).get

    updatedEntity.likesCount === 20
    updatedEntity.usersWhoSetLikes === likers.take(20)
  }

  @Test
  def findDateSortedWishes {
    val userId = UserId()
    val testDateGenerator = new TestDateGenerator

    val wish1, wish2, wish3 = new WishFixture(userId, creationDate = testDateGenerator.next()).wish
    val notOrdered = Seq(wish2, wish3, wish1)
    notOrdered foreach service.save
    val foundWishes = service.find(userId, offsetLimit = OffsetLimit(), fulfilled = false)
    foundWishes === Seq(wish3, wish2, wish1)
  }
  @Test
  def findWishes {
    val wish = (new WishFixture).wish
    service.save(wish)
    service.findWishes(Set(wish.id)) === Seq(wish)
  }
  @Test
  def markAsDeleted {
    val wc = 10

    val (pid, lid) = (new WishFixture().productId, new WishFixture().locationId)
    val wishes = (1 to wc) map (_ => new WishFixture().wish.copy(product = ProductRef(lid, pid)))

    wishes.foreach(w => service.save(w))
    service.markWishesAsDeleted(pid)

    val query = Query.query(new Criteria("p.pid").is(pid.id).and("lfs").is("r"))
    val updated = WrapAsScala.asScalaBuffer(service.mongo.find(query, service.entityClass)).toList

    updated.length === wishes.length
    updated.length === wc

    service.mongo.remove(query, service.entityClass)
  }
}

class FulfillWishFixture(fixtureUserId: ObjectId = new ObjectId) {
  val userId = UserId(fixtureUserId)
  val productId = ProductId(new ObjectId)
  val locationId = LocationId(new ObjectId)
  val fulfillmentDate = DateHelper.currentDate

  def createWish(createdAt: Date = DateHelper.currentDate) = Wish(
    userId = userId,
    product = ProductRef(
      locationId = locationId,
      productId = productId
    ),
    fulfillmentDate = None,
    creationDate = createdAt,
    lifecycleStatus = None
  )
}

class WishFixture(fixtureUserId: UserId = new UserId(),
                  fixtureFulfillmentDate: Option[Date] = None,
                  likesCount: Int = 0,
                  creationDate: Date = DateHelper.currentDate,
                  usersWhoSetLikes: Seq[UserId] = Nil) {
  val userId = fixtureUserId
  val locationId = LocationId(new ObjectId)
  val productId = ProductId(new ObjectId)
  val wish = Wish(
    userId = userId,
    product = ProductRef(
      locationId = locationId,
      productId = productId
    ),
    creationDate = creationDate,
    fulfillmentDate = fixtureFulfillmentDate,
    likesCount = likesCount,
    usersWhoSetLikes = usersWhoSetLikes,
    lifecycleStatus = Some(WishLifecycleId.Removed.id)
  )
}