package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.FriendshipRequest
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.{UserId, FriendshipRequestId}
import com.tooe.core.util.{TestDateGenerator, DateHelper}
import java.util.Date
import com.tooe.api.service.OffsetLimit

class FriendshipRequestDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: FriendshipRequestDataService = _

  lazy val entities = new MongoDaoHelper("friendshiprequest")

  @Test
  def saveAndRead {
    val entity = new FriendshipRequestFixture().friendshipRequest
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def delete {
    val entity = new FriendshipRequestFixture().friendshipRequest
    service.save(entity)
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = new FriendshipRequestFixture().friendshipRequest
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "uid" : ${entity.userId.id.mongoRepr} ,
      "aid" : ${entity.actorId.id.mongoRepr} ,
      "t" : ${entity.createdAt.mongoRepr}
    }""")
  }

  @Test
  def findUsersFriendshipRequests {
    val userId = UserId()
    val dateGen = new TestDateGenerator
    val r1, r2, r3 = new FriendshipRequestFixture(userId = userId, createdAt = dateGen.next()).friendshipRequest
    Seq(r2, r1, r3) foreach { r => service.save(r) }

    service.findByUser(userId, OffsetLimit()) === Seq(r1, r2, r3)
    service.findByUser(userId, OffsetLimit(offset = 1, limit = 1)) === Seq(r2)
  }

  @Test
  def findUserFriendshipOffer {
    val f = new FriendshipRequestFixture
    import f._
    service.save(friendshipRequest)

    service.find(userId = friendshipRequest.userId, actorId = friendshipRequest.actorId) === Some(friendshipRequest)
    service.find(userId = friendshipRequest.actorId, actorId = friendshipRequest.userId) === None
  }
}

class FriendshipRequestFixture(userId: UserId = UserId(), actorId: UserId = UserId(), createdAt: Date = DateHelper.currentDate) {
  val friendshipRequest =
    FriendshipRequest(
      id = FriendshipRequestId(),
      userId = userId,
      actorId = actorId,
      createdAt = createdAt
    )
}