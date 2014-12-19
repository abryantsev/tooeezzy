package com.tooe.core.service

import com.tooe.core.db.mongo.domain.AdminUserEvent
import com.tooe.core.domain.{AdminUserId, AdminUserEventId}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.AdminUserEventRepository
import org.springframework.data.mongodb.core.MongoTemplate
import scala.collection.JavaConverters._
import org.springframework.data.mongodb.core.query.{Criteria, Query}

trait AdminUserEventDataService {

  def save(event: AdminUserEvent): AdminUserEvent

  def findOne(id: AdminUserEventId): Option[AdminUserEvent]

  def findByUser(userId: AdminUserId): Seq[AdminUserEvent]

  def delete(userId: AdminUserId, id: AdminUserEventId): Unit

  def deleteByUser(userId: AdminUserId): Unit

}

@Service
class AdminUserEventDataServiceImpl extends AdminUserEventDataService {
  @Autowired var repo: AdminUserEventRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[AdminUserEvent]

  def save(event: AdminUserEvent) = repo.save(event)

  def findOne(id: AdminUserEventId): Option[AdminUserEvent] = Option(repo.findOne(id.id))

  def findByUser(userId: AdminUserId) = {
     mongo.find(Query.query(new Criteria("uid").is(userId.id)), entityClass).asScala
  }

  def delete(userId: AdminUserId, id: AdminUserEventId) {
    mongo.remove(Query.query(Criteria.where("_id").is(id.id).and("uid").is(userId.id)), entityClass)
  }

  def deleteByUser(userId: AdminUserId) {
    mongo.remove(Query.query(new Criteria("uid").is(userId.id)), entityClass)
  }

}
