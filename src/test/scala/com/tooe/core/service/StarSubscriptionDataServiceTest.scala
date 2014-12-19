package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.domain._
import scala.Some
import com.tooe.core.db.mongo.domain.StarSubscription
import com.tooe.api.service.OffsetLimit

class StarSubscriptionDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: StarSubscriptionDataService = _

  lazy val entities = new MongoDaoHelper("star_subscription")

  @Test
  def saveAndRead {
    val f = new StarSubscriptionFixture
    import f._
    service.find(starSubscription.id) === None
    service.save(starSubscription)
    service.find(starSubscription.id) === Some(starSubscription)
  }

  @Test
  def representation {
    val f = new StarSubscriptionFixture
    import f._
    service.save(starSubscription)
    val repr = entities.findOne(starSubscription.id.id)

    jsonAssert(repr)( s"""{
       "_id" : ${starSubscription.id.id.mongoRepr}  ,
       "uid" : ${starSubscription.userId.id.mongoRepr} ,
       "sid" : ${starSubscription.starId.id.mongoRepr}
     }""")
  }


  @Test
  def find {
    val userId = UserId()
    val usersStars = generateEntities(count = 2, userId = userId)
    saveEntities(usersStars ++ generateEntities(2))
    val foundStarSubscription = service.findByUser(userId)
    foundStarSubscription must haveSize(2)
    foundStarSubscription must haveTheSameElementsAs(usersStars)
  }

  @Test
  def existSubscribe {
    val userId = UserId()
    val starId = UserId()
    val otherUserId = UserId()
    service.save(getEntity(userId, starId))

    service.existSubscribe(starId, userId) === true
    service.existSubscribe(starId, otherUserId) === false
  }

  @Test
  def findStarSubscribers {
    val starId = UserId()
    val entities = (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription.copy(starId = starId) }
    entities.foreach(service.save)
    (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription }.foreach(service.save)
    val starsSubscribers = service.findStarSubscribers(starId, OffsetLimit(0, 10))
    starsSubscribers must haveSize(3)
    starsSubscribers must haveTheSameElementsAs(entities)
  }

  @Test
  def countStarSubscribers {
    val starId = UserId()
    val entities = (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription.copy(starId = starId) }
    entities.foreach(service.save)
    (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription }.foreach(service.save)
    service.countStarSubscribers(starId) === 3
  }

  @Test
  def getStarsByUserSubscription {
    val userId = UserId()
    val entities = (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription.copy(userId = userId) }
    entities.foreach(service.save)
    (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription }.foreach(service.save)

    service.getStarsByUserSubscription(userId, OffsetLimit()) === entities
    service.getStarsByUserSubscription(userId, OffsetLimit(0, 2)) === entities.take(2)
  }

  @Test
  def getStarsByUserSubscriptionCount {
    val userId = UserId()
    val entities = (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription.copy(userId = userId) }
    entities.foreach(service.save)
    (1 to 3).map { _ => new StarSubscriptionFixture().starSubscription }.foreach(service.save)

    service.getStarsByUserSubscriptionCount(userId) === entities.size
  }
  
  def getEntity(userId: UserId = UserId(), starId: UserId = UserId()) =
    StarSubscription(userId = userId, starId = starId)

  def generateEntities(count: Int, userId: UserId = UserId(), starId: UserId = UserId()) =
    (1 to count).map(v => getEntity(userId, starId))

  def saveEntities(subscriptions: Seq[StarSubscription]) { subscriptions.foreach(service.save) }

}

class StarSubscriptionFixture{
  val userId = UserId()
  val starId = UserId()

  val starSubscription = StarSubscription(
    userId = userId,
    starId = starId
  )
}

