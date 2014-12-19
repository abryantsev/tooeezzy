package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationSubscription
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.repository.LocationSubscriptionRepository
import com.tooe.core.domain.{LocationId, UserId, LocationSubscriptionId}
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query._
import scala.collection.JavaConverters._

trait LocationSubscriptionDataService {
  def save(entity: LocationSubscription): LocationSubscription
  def findOne(id: LocationSubscriptionId): Option[LocationSubscription]
  def remove(userId: UserId, locationId: LocationId): Unit
  def existSubscription(userId: UserId, locationId: LocationId): Boolean
  def findLocationSubscriptionsByLocation(locationId: LocationId, offsetLimit: OffsetLimit): Seq[LocationSubscription]
  def findLocationSubscriptionsByUser(userId: UserId, offsetLimit: OffsetLimit): Seq[LocationSubscription]
  def countLocationSubscribers(locationId: LocationId): Long
}

@Service
class LocationSubscriptionDataServiceImpl extends LocationSubscriptionDataService {
  @Autowired var repo: LocationSubscriptionRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[LocationSubscription]

  def save(entity: LocationSubscription) = repo.save(entity)

  def findOne(id: LocationSubscriptionId) = Option(repo.findOne(id.id))

  def remove(userId: UserId, locationId: LocationId) {
    val query = Query.query(new Criteria("uid").is(userId.id).and("lid").is(locationId.id))
    mongo.remove(query, entityClass)
  }

  def existSubscription(userId: UserId, locationId: LocationId) = {
    val query = Query.query(new Criteria("uid").is(userId.id).and("lid").is(locationId.id))
    mongo.findOne(query, entityClass) != null
  }

  def findLocationSubscriptionsByLocation(locationId: LocationId, offsetLimit: OffsetLimit) = {
    mongo.find(getByLocationQuery(locationId).withPaging(offsetLimit), entityClass).asScala
  }
  
  def findLocationSubscriptionsByUser(userId: UserId, offsetLimit: OffsetLimit) = {
    mongo.find(Query.query(new Criteria("uid").is(userId.id)).withPaging(offsetLimit), entityClass).asScala
  }

  def getByLocationQuery(locationId: LocationId): Query = {
    Query.query(new Criteria("lid").is(locationId.id))
  }

  def countLocationSubscribers(locationId: LocationId) = mongo.count(getByLocationQuery(locationId), entityClass)

}
