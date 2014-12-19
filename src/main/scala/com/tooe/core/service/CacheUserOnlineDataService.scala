package com.tooe.core.service

import com.tooe.core.domain.{OnlineStatusId, UserId}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import com.tooe.core.db.mongo.domain.CacheUserOnline
import scala.collection.JavaConverters._
import com.tooe.core.util.ProjectionHelper._
import java.util.Date
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query._

trait CacheUserOnlineDataService {

  def upsert(id: UserId, createdAt: Date, onlineStatusId: OnlineStatusId): UpsertResult

  def updateOnlineStatus(userId: UserId, status: OnlineStatusId): UpdateResult

  def updateFriends(userId: UserId, friends: Seq[UserId]): UpdateResult

  def addFriend(userId: UserId, friend: UserId): UpdateResult

  def removeFriend(userId: UserId, friend: UserId): UpdateResult

  def delete(userId: UserId): Unit
  
  def find(userId: UserId): Option[CacheUserOnline]

  def getUsersStatuses(userIds: Seq[UserId]): Map[UserId, OnlineStatusId]

  def findOnlineUsers(userIds: Seq[UserId]): Set[UserId]

  def save(entity: CacheUserOnline): Unit

  //TODO test
  def getOnlineFriends(userId: UserId, offsetLimit: OffsetLimit): Seq[UserId]

  //TODO test
  def countOnlineFriends(userId: UserId): Long
}

@Service
class CacheUserOnlineDataServiceImpl extends CacheUserOnlineDataService {
  import com.tooe.core.db.mongo.query._

  @Autowired var mongo: MongoTemplate = _
  
  val entityClass = classOf[CacheUserOnline]

  def upsert(id: UserId, createdAt: Date, status: OnlineStatusId) = {
    val update = new Update()
      .set("t", createdAt)
      .set("os", status.id)
    mongo.upsert(findByUserIdQuery(id), update, entityClass).asUpsertResult
  }

  def updateOnlineStatus(userId: UserId, status: OnlineStatusId) = {
    val update = new Update().set("os", status.id)
    mongo.updateFirst(findByUserIdQuery(userId), update, entityClass).asUpdateResult
  }

  def updateFriends(userId: UserId, friends: Seq[UserId]) = {
    val update = new Update().set("fs", friends.map(_.id).asJavaCollection)
    mongo.updateFirst(findByUserIdQuery(userId), update, entityClass).asUpdateResult
  }

  def addFriend(userId: UserId, friend: UserId) = {
    val update = new Update().addToSet("fs", friend.id)
    mongo.updateFirst(findByUserIdQuery(userId), update, entityClass).asUpdateResult
  }

  def removeFriend(userId: UserId, friend: UserId) = {
    val update = new Update().pull("fs", friend.id)
    mongo.updateFirst(findByUserIdQuery(userId), update, entityClass).asUpdateResult
  }

  def delete(userId: UserId) = mongo.remove(findByUserIdQuery(userId), entityClass)
  
  def find(userId: UserId) = Option(mongo.findOne(findByUserIdQuery(userId), entityClass))
  
  private def findByUserIdQuery(userId: UserId) = new Query(new Criteria("_id").is(userId.id))

  def getUsersStatuses(userIds: Seq[UserId]) = {
    val query = new Query(new Criteria("_id").in(userIds.map(_.id).asJavaCollection)).extendProjection(Set("_id", "os"))
    mongo.find(query, entityClass).asScala.map(status => (status.id, status.onlineStatusId)).toMap
  }

  def findOnlineUsers(userIds: Seq[UserId]) = {
    val query = new Query(new Criteria("_id").in(userIds.map(_.id).asJavaCollection)
      .andOperator(userOnlineCriteria)
    ).extendProjection(Set("_id"))
    mongo.find(query, entityClass).asScala.map(_.id).toSet
  }

  def save(entity: CacheUserOnline) = mongo.save(entity)

  def getOnlineFriends(userId: UserId, offsetLimit: OffsetLimit) = {
    val query = getFriendsOnlineQuery(userId).withPaging(offsetLimit).extendProjection(Set("_id"))
    mongo.find(query, entityClass).asScala.map(_.id).toSeq
  }

  def getFriendsOnlineQuery(userId: UserId): Query = {
    new Query(
      userOnlineCriteria
        .and("fs").in(userId.id))
  }

  def userOnlineCriteria: Criteria = {
    new Criteria("os").in(Seq(OnlineStatusId.Online, OnlineStatusId.ReadyForChat).map(_.id).asJavaCollection)
  }

  def countOnlineFriends(userId: UserId) =  mongo.count(getFriendsOnlineQuery(userId), entityClass)
}