package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.FriendsCache
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.{UserId, UserGroupType}

class FriendsCacheDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: FriendsCacheDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("cache_friends")

  @Test
  def saveAndRead {
    val entity = FriendsCache()
    service.findOne(entity.id.id) === None
    service.save(entity) === entity
    service.findOne(entity.id.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = FriendsCache()
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}"} ,
      "uid" : { "$$oid" : "${entity.userId.id}"} ,
      "t" : ${entity.creationTime.mongoRepr} ,
      "fs" : []
    }""")
  }

  @Test
  def findFriendsInCache {
    val entity = FriendsCache(friendGroupId = Some(UserGroupType.BestFriend.id.toUpperCase))
    service.save(entity)
    service.findFriendsInCache(entity.userId, entity.friendGroupId.getOrElse(UserGroupType.Friend.id).toUpperCase) === Some(entity)
  }

  @Test
  def addAndRemoveFriends() {
    val friends = (1 to 5).map(_ => UserId()).toSeq
    val entity = FriendsCache()
    service.save(entity)

    service.addUsersToFriends(entity.userId, friends)

    service.findOne(entity.id.id).map(_.friends).getOrElse(Seq.empty) === friends

    service.removeUsersFromFriends(entity.userId, friends)

    service.findOne(entity.id.id) === Some(entity)
  }

  @Test
  def addUserToSomeGroup {

    val userId = UserId()
    val friendId = UserId()
    val group = UserGroupType.Family

    val entity1 = FriendsCache(userId = userId)
    val entity2 = FriendsCache(userId = userId, friendGroupId = Some(group.id.toUpperCase))
    service.save(entity1)
    service.save(entity2)

    service.addUserToGroup(userId, friendId, Seq(group))

    service.findOne(entity1.id.id).map(_.friends) === None
    service.findOne(entity2.id.id).map(_.friends).toSeq === Seq(friendId)

  }


  @Test
  def removeUserFromSomeGroup {

    val userId = UserId()
    val friendId = UserId()
    val group = UserGroupType.Family

    val entity1 = FriendsCache(userId = userId, friends = Seq(friendId))
    val entity2 = FriendsCache(userId = userId, friendGroupId = Some(group.id.toUpperCase), friends = Seq(friendId))
    service.save(entity1)
    service.save(entity2)

    service.addUserToGroup(userId, friendId, Seq(group))

    service.findOne(entity1.id.id).map(_.friends).toSeq === Seq(friendId)
    service.findOne(entity2.id.id).map(_.friends) === None


  }

}