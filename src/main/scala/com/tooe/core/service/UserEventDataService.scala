package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.UserEventRepository
import com.tooe.core.domain._
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query._
import org.springframework.data.mongodb.core.query.{Criteria, Query, Update}
import org.springframework.data.mongodb.core.{FindAndModifyOptions, MongoTemplate}
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.UserEvent
import com.tooe.core.domain.UserEventId

trait UserEventDataService {
  def save(entity: UserEvent): UserEvent

  def saveMany(entities: Seq[UserEvent]): Seq[UserEvent]

  def findOne(id: UserEventId): Option[UserEvent]

  def find(friendshipRequestId: FriendshipRequestId): Option[UserEvent]

  def find(userId: UserId, skipLimitSort: SkipLimitSort): Seq[UserEvent]

  def updateStatus(id: UserEventId, status: UserEventStatus): Option[UserEvent]

  def delete(id: UserEventId): Unit

  def delete(userId: UserId): Unit

  def unsetFriendshipRequestId(id: UserEventId): UpdateResult
}

@Service
class UserEventDataServiceImpl extends UserEventDataService {
  @Autowired var repo: UserEventRepository = _
  @Autowired var mongo: MongoTemplate = _

  def save(entity: UserEvent) = repo.save(entity)

  def saveMany(entities: Seq[UserEvent]) = repo.save(entities.asJava).asScala

  def findOne(id: UserEventId) = Option(repo.findOne(id.id))

  def find(friendshipRequestId: FriendshipRequestId) =
    repo.findByFriendshipRequestId(friendshipRequestId.id).asScala.headOption

  def find(userId: UserId, skipLimitSort: SkipLimitSort) =
    repo.findByUserId(userId.id, skipLimitSort).asScala

  def updateStatus(id: UserEventId, status: UserEventStatus) = Option(mongo.findAndModify(
    Query.query(new Criteria("_id").is(id.id)),
    Update.update("cs", status.id),
    new FindAndModifyOptions().returnNew(true),
    classOf[UserEvent]
  ))

  def delete(id: UserEventId) = repo.delete(id.id)

  def delete(userId: UserId) = mongo.remove(Query.query(new Criteria("uid").is(userId.id)), classOf[UserEvent])

  def unsetFriendshipRequestId(id: UserEventId) = {
    val query = Query.query(new Criteria("_id").is(id.id))
    val update = new Update().unset("if")
    mongo.updateFirst(query, update, classOf[UserEvent]).asUpdateResult
  }
}