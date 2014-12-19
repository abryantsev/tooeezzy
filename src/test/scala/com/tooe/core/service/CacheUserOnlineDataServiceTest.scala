package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.{OnlineStatusId, UserId}
import com.tooe.core.util.{TestDateGenerator, DateHelper}
import com.tooe.core.db.mongo.domain.CacheUserOnline
import com.tooe.core.db.mongo.query.{UpdateResult, UpsertResult}
import com.tooe.api.service.OffsetLimit

class CacheUserOnlineDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: CacheUserOnlineDataService = _

  lazy val entities = new MongoDaoHelper("cache_useronline")

  @Test
  def upsert {
    val userId = UserId()
    val d1, d2 = new TestDateGenerator().next()

    service.find(userId) === None

    service.upsert(userId, d1, OnlineStatusId.Online) === UpsertResult.Inserted

    val found1 = service.find(userId).get
    found1.id === userId
    found1.createdAt === d1
    found1.onlineStatusId === OnlineStatusId.Online
    found1.friends === Nil

    service.upsert(userId, d2, OnlineStatusId.Busy) === UpsertResult.Updated

    val found2 = service.find(userId).get
    found2.id === userId
    found2.createdAt === d2
    found2.onlineStatusId === OnlineStatusId.Busy
    found2.friends === Nil
  }

  @Test
  def updateOnlineStatus {
    val f = new CacheUserOnlineDataServiceFixture(onlineStatusId = OnlineStatusId.Online)
    import f._

    service.updateOnlineStatus(entity.id, OnlineStatusId.Busy) === UpdateResult.NotFound
    service.find(entity.id) === None

    service.save(entity)
    service.updateOnlineStatus(entity.id, OnlineStatusId.Busy) === UpdateResult.Updated
    service.find(entity.id).get === entity.copy(onlineStatusId = OnlineStatusId.Busy)
  }

  @Test
  def updateFriends {
    val entity = new CacheUserOnlineDataServiceFixture().entity.copy(friends = Seq(UserId()))
    val newFriends = Seq(UserId())

    service.updateFriends(entity.id, newFriends) === UpdateResult.NotFound
    service.find(entity.id) === None

    service.save(entity)
    service.updateFriends(entity.id, newFriends) === UpdateResult.Updated
    service.find(entity.id).get === entity.copy(friends = newFriends)
  }

  @Test
  def addFriend {
    val entity = new CacheUserOnlineDataServiceFixture().entity.copy(friends = Seq(UserId()))
    val newFriend = UserId()

    service.addFriend(entity.id, newFriend) === UpdateResult.NotFound
    service.find(entity.id) === None

    service.save(entity)
    service.addFriend(entity.id, newFriend) === UpdateResult.Updated

    service.find(entity.id).get.friends.toSet === (entity.friends :+ newFriend).toSet
  }

  @Test
  def removeFriend {
    val f1, f2 = UserId()
    val entity = new CacheUserOnlineDataServiceFixture().entity.copy(friends = Seq(f1, f2))

    service.removeFriend(entity.id, f1) === UpdateResult.NotFound
    service.find(entity.id) === None

    service.save(entity)
    service.removeFriend(entity.id, f1) === UpdateResult.Updated

    service.find(entity.id).get.friends === Seq(f2)
  }

  @Test
  def saveReadDelete {
    val f = new CacheUserOnlineDataServiceFixture
    import f._
    service.save(entity)
    service.find(entity.id) === Some(entity)

    service.delete(entity.id)

    service.find(entity.id) === None
  }

  @Test
  def representation {
    val f = new CacheUserOnlineDataServiceFixture
    import f._
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "fs" : ${entity.friends.map(_.id).map(_.mongoRepr).mkString("[", ", ", "]")} ,
      "t" : ${entity.createdAt.mongoRepr},
      "os" : ${entity.onlineStatusId.id}
    }""")
  }

  @Test
  def getUsersStatuses() {
    def cacheUserOnline = new CacheUserOnlineDataServiceFixture().entity
    val cacheUser, cacheUser2 = cacheUserOnline
    service.save(cacheUser)
    service.save(cacheUser2)
    service.getUsersStatuses(Seq(cacheUser, cacheUser2).map(_.id))  === Map(cacheUser.id -> cacheUser.onlineStatusId, cacheUser2.id -> cacheUser2.onlineStatusId)
  }

  @Test
  def findOnlineUsers() {
    def cacheUserOnline(onlineStatus: OnlineStatusId) =
      new CacheUserOnlineDataServiceFixture(onlineStatusId = onlineStatus).entity

    val cacheUser, cacheUser2 = cacheUserOnline(OnlineStatusId.Online)
    val cacheUser3 = cacheUserOnline(OnlineStatusId.Busy)

    service.save(cacheUser)
    service.save(cacheUser2)
    service.save(cacheUser3)

    service.findOnlineUsers(Seq(cacheUser, cacheUser2, cacheUser3).map(_.id)) === Set(cacheUser.id, cacheUser2.id)
  }

  @Test
  def getOnlineFriends {
    val userId = UserId()
    val cache1 = new CacheUserOnlineDataServiceFixture(friends = Seq(userId)).entity
    val cache2 = new CacheUserOnlineDataServiceFixture(friends = Seq(userId)).entity
    val cache3 = new CacheUserOnlineDataServiceFixture().entity

    Seq(cache1, cache2, cache3).foreach(service.save)

    val friendsOnline = service.getOnlineFriends(userId, OffsetLimit())
    friendsOnline must haveSize(2)
    friendsOnline must haveTheSameElementsAs(Seq(cache1, cache2).map(_.id))
  }

  @Test
  def countOnlineFriends {
    val userId = UserId()
    val cache1 = new CacheUserOnlineDataServiceFixture(friends = Seq(userId)).entity
    val cache2 = new CacheUserOnlineDataServiceFixture(friends = Seq(userId)).entity
    val cache3 = new CacheUserOnlineDataServiceFixture().entity

    Seq(cache1, cache2, cache3).foreach(service.save)

    service.countOnlineFriends(userId) === 2
  }

}

class CacheUserOnlineDataServiceFixture(onlineStatusId: OnlineStatusId = OnlineStatusId.Online, friends: Seq[UserId] = Seq(UserId())) {
  val entity = CacheUserOnline(
    id = UserId(),
    createdAt = DateHelper.currentDate,
    onlineStatusId = onlineStatusId,
    friends = friends
  )
}