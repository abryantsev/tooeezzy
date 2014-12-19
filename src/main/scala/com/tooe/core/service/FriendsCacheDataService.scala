package com.tooe.core.service

import com.tooe.core.db.mongo.domain.FriendsCache
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.FriendsCacheRepository
import org.bson.types.ObjectId
import com.tooe.core.domain.{UserGroupType, UserId}
import com.tooe.core.db.mongo.query.{WriteResultHelper, UpdateResult}
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import scala.collection.JavaConverters._


trait FriendsCacheDataService extends DataService[FriendsCache, ObjectId] {

  def findFriendsInCache(userId: UserId, friendsGroup: String): Option[FriendsCache]

  def addUsersToFriends(userId: UserId, friends: Seq[UserId]): UpdateResult

  def removeUsersFromFriends(userId: UserId, friends: Seq[UserId]): UpdateResult

  def removeUserFromGroup(userId: UserId, friendId: UserId, groups: Seq[UserGroupType]): Unit

  def addUserToGroup(userId: UserId, friendId: UserId, groups: Seq[UserGroupType]): Unit
}

@Service
class FriendsCacheDataServiceImpl extends FriendsCacheDataService with DataServiceImpl[FriendsCache, ObjectId] {
  @Autowired var repo: FriendsCacheRepository = _
  @Autowired var mongo: MongoTemplate = _

  private val entityClass = classOf[FriendsCache]

  def findFriendsInCache(userId: UserId, friendsGroup: String): Option[FriendsCache] = Option(repo.findFriendsInCache(userId.id, friendsGroup))

  def addUsersToFriends(userId: UserId, friends: Seq[UserId]) = {
    val query = Query.query(Criteria.where("uid").is(userId.id))
    val update = new Update().pushAll("fs", friends.map(_.id).toArray)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def removeUsersFromFriends(userId: UserId, friends: Seq[UserId]) = {
    val query = Query.query(Criteria.where("uid").is(userId.id))
    val update = new Update().pullAll("fs", friends.map(_.id).toArray)
    mongo.updateFirst(query, update, entityClass).asUpdateResult
  }

  def removeUserFromGroup(userId: UserId, friendId: UserId, groups: Seq[UserGroupType]) {
    val query = Query.query(Criteria.where("uid").is(userId.id).and("gid").in(groups.map(_.id.toUpperCase).asJavaCollection))
    val update = new Update().pull("fs", friendId.id)
    mongo.updateMulti(query, update, entityClass)

  }

  def addUserToGroup(userId: UserId, friendId: UserId, groups: Seq[UserGroupType]) {
    val query = Query.query(Criteria.where("uid").is(userId.id).and("gid").in(groups.map(_.id.toUpperCase).asJavaCollection))
    val update = new Update().addToSet("fs", friendId.id)
    mongo.updateMulti(query, update, entityClass)
  }

}
