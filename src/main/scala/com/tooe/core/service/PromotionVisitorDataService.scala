package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PromotionVisitorRepository
import com.tooe.core.db.mongo.domain.PromotionVisitor
import com.tooe.core.domain.{PromotionStatus, UserId, PromotionId, PromotionVisitorId}
import java.util.Date
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query._
import com.tooe.api.service.OffsetLimit

trait PromotionVisitorDataService {
  def save(entity: PromotionVisitor): PromotionVisitor
  def findOne(id: PromotionVisitorId): Option[PromotionVisitor]
  def findAllVisitors(id: PromotionId, offsetLimit: OffsetLimit): Seq[PromotionVisitor]
  def countAllVisitors(id: PromotionId): Long
  def findAllVisitorIds(id: PromotionId): Set[UserId]
  def findVisitors(promotionIds: Set[PromotionId]): Seq[PromotionVisitor]
  def upsertStatus(promotionId: PromotionId, visitor: UserId, date: Date, status: PromotionStatus): UpdateResult
  def find(promotionId: PromotionId, userId: UserId): Option[PromotionVisitor]
}

@Service
class PromotionVisitorDataServiceImpl extends PromotionVisitorDataService {
  @Autowired var repo: PromotionVisitorRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[PromotionVisitor]

  def save(entity: PromotionVisitor) = repo.save(entity)

  def findOne(id: PromotionVisitorId) = Option(repo.findOne(id.id))

  def findAllVisitors(id: PromotionId, offsetLimit: OffsetLimit): Seq[PromotionVisitor] =
    repo.findAllVisitors(id.id, SkipLimitSort(offsetLimit).desc("ds.st").asc("_id")).asScala.toSeq

  def findAllVisitorIds(id: PromotionId) = repo.findAllVisitorUserIds(id.id).asScala.map(_.visitor).toSet

  def countAllVisitors(id: PromotionId) = mongo.count(Query.query(criteriaByPromotionId(id)), entityClass)

  private def criteriaByPromotionId(id: PromotionId) = new Criteria("pid").is(id.id)

  def upsertStatus(promotionId: PromotionId, visitor: UserId, date: Date, status: PromotionStatus) =
    mongo.upsert(
      Query.query(criteriaByPromotionId(promotionId).and("uid").is(visitor.id)),
      new Update().set("t", date).set("s", status.id),
      entityClass
    ).asUpdateResult

  def find(promotionId: PromotionId, userId: UserId) =
    repo.find(promotionId = promotionId.id, userId = userId.id).asScala.headOption

  def findVisitors(promotionIds: Set[PromotionId]) = repo.findByIds(promotionIds.map(_.id).asJava).asScala.toSeq
}