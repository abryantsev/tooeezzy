package com.tooe.core.service

import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.domain.StarSubscription
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.domain.{StarSubscriptionId,  UserId}
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.db.mongo.repository.StarSubscriptionRepository
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query._


trait StarSubscriptionDataService {
  def save(entity: StarSubscription): StarSubscription
  def find(id: StarSubscriptionId): Option[StarSubscription]
  def removeSubscription(userId: UserId, starId: UserId)
  def findByUser(userId: UserId): Seq[StarSubscription]
  def existSubscribe(starId: UserId, userId: UserId): Boolean
  def findStarSubscribers(starId: UserId, offsetLimit: OffsetLimit): Seq[StarSubscription]
  def countStarSubscribers(starId: UserId): Long
  def getStarsByUserSubscription(userId: UserId, offsetLimit: OffsetLimit): Seq[StarSubscription]
  def getStarsByUserSubscriptionCount(userId: UserId): Long
}

@Service
class StartSubscriptionDataServiceImpl extends StarSubscriptionDataService {
  @Autowired var repo: StarSubscriptionRepository = _

  @Autowired var mongo: MongoTemplate = _

  def save(entity: StarSubscription): StarSubscription = repo.save(entity)

  def find(id: StarSubscriptionId) = Option(repo.findOne(id.id))

  def removeSubscription(userId: UserId, starId: UserId) {
    val query = Query.query(new Criteria("uid").is(userId.id).and("sid").is(starId.id))
    mongo.remove(query, classOf[StarSubscription])
  }

  def findByUser(userId: UserId) = repo.findByUser(userId.id).asScala.toList

  def existSubscribe(starId: UserId, userId: UserId) = {
    mongo.findOne(Query.query(new Criteria("uid").is(userId.id).and("sid").is(starId.id)), classOf[StarSubscription]) != null
  }

  def findStarSubscribers(starId: UserId, offsetLimit: OffsetLimit) = {
    mongo.find(
     starSubscribersQuery(starId).withPaging(offsetLimit),
      classOf[StarSubscription]
    ).asScala
  }


  def starSubscribersQuery(starId: UserId): Query = {
    Query.query(new Criteria("sid").is(starId.id))
  }

  def countStarSubscribers(starId: UserId) =  mongo.count(starSubscribersQuery(starId), classOf[StarSubscription])

  def getStarsByUserSubscription(userId: UserId, offsetLimit: OffsetLimit) = {
    mongo.find(Query.query(Criteria.where("uid").is(userId.id)).withPaging(offsetLimit), classOf[StarSubscription]).asScala
  }

  def getStarsByUserSubscriptionCount(userId: UserId) = {
    mongo.count(Query.query(Criteria.where("uid").is(userId.id)), classOf[StarSubscription])
  }

}