package com.tooe.core.service

import com.tooe.core.db.mongo.domain.FriendshipRequest
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.FriendshipRequestRepository
import com.tooe.core.domain.{UserId, FriendshipRequestId}
import com.tooe.api.service.OffsetLimit
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query.SkipLimitSort

trait FriendshipRequestDataService {

  def findOne(id: FriendshipRequestId): Option[FriendshipRequest]

  def save(entity: FriendshipRequest): FriendshipRequest

  def delete(id: FriendshipRequestId): Unit

  def findByUser(userId: UserId, offsetLimit: OffsetLimit): Seq[FriendshipRequest]

  def find(userId: UserId, actorId: UserId): Option[FriendshipRequest]
}

@Service
class FriendshipRequestDataServiceImpl extends FriendshipRequestDataService {
  @Autowired var repo: FriendshipRequestRepository = _

  val entityClass = classOf[FriendshipRequest]

  def findOne(id: FriendshipRequestId) = Option(repo.findOne(id.id))

  def save(entity: FriendshipRequest) = repo.save(entity)

  def delete(id: FriendshipRequestId) = repo.delete(id.id)

  def findByUser(userId: UserId, offsetLimit: OffsetLimit) =
    repo.findByUserId(userId = userId.id, SkipLimitSort(offsetLimit).asc("t")).asScala

  def find(userId: UserId, actorId: UserId) = repo.find(userId = userId.id, actorId = actorId.id).asScala.headOption
}