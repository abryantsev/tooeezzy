package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.WishLike
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.bson.types.ObjectId
import com.tooe.core.util.DateHelper
import com.tooe.core.domain.{WishId, UserId}
import java.util.Date
import com.tooe.core.util.TestDateGenerator
import com.tooe.api.service.OffsetLimit

class WishLikeDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: WishLikeDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("wish_likes")

  def fullWishLike(wishId: WishId = WishId(), createdAt: Date = DateHelper.currentDate) = WishLike(
    wishId = wishId,
    created = createdAt,
    userId = UserId(new ObjectId)
  )

  @Test
  def saveAndRead {
    val entity = fullWishLike()
    service.findOne(entity.id) === None
    service.save(entity)
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def saveAndDelete {
    val entity = fullWishLike()
    service.save(entity)
    service.delete(entity.wishId, entity.userId)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = fullWishLike()
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr="+repr)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
      "wid" : { "$$oid" : "${entity.wishId.id.toString}" } ,
      "t" : ${entity.created.mongoRepr} ,
      "uid" : { "$$oid" : "${entity.userId.id.toString}" }
    }""")
  }

  @Test
  def lastLikes {
    val generator = new TestDateGenerator()
    val wishId = new WishId
    val like1 = fullWishLike(wishId, generator.next())
    val like2 = fullWishLike(wishId, generator.next())
    val like0 = fullWishLike(createdAt = generator.next())
    val like3 = fullWishLike(wishId, generator.next())

    service.save(like0)
    service.save(like2)
    service.save(like1)
    service.save(like3)

    val wshs = service.wishLikes(wishId, OffsetLimit(1, 1))
    service.wishLikes(wishId, OffsetLimit(1, 1)) === Seq(like2)
  }

  @Test
  def likesQty {
    val wishId = new WishId
    val like0, like1, like2, like3 = fullWishLike(wishId)

    service.save(like0)
    service.save(like1)
    service.save(like2)
    service.save(like3)

    service.likesQty(wishId) === 4
  }

  @Test
  def userLikeExists {
    val wishId = WishId()
    val userId = UserId()
    val particularUserWishLike = WishLike(wishId = wishId,
                                          created = new Date,
                                          userId = userId)
    service.userLikeExists(wishId, userId) === false
    service.save(particularUserWishLike)
    service.userLikeExists(wishId, userId) === true
  }
}