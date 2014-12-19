package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.domain.{LocationId, UserId, LocationSubscriptionId}
import com.tooe.core.db.mongo.domain.LocationSubscription
import com.tooe.api.service.OffsetLimit

class LocationSubscriptionDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service:  LocationSubscriptionDataService = _

  def getEntity(id: LocationSubscriptionId = LocationSubscriptionId(), userId: UserId = UserId(), locationId: LocationId = LocationId()) =
    LocationSubscription(id, userId, locationId)

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def delete {
    val entity = getEntity()
    service.save(entity)
    service.findOne(entity.id) === Some(entity)
    service.remove(entity.userId, entity.locationId)
    service.findOne(entity.id) === None
  }

  @Test
  def existSubscription {
    val entity = getEntity()
    service.existSubscription(entity.userId, entity.locationId) === false
    service.save(entity)
    service.existSubscription(entity.userId, entity.locationId) === true
  }

  @Test
  def findLocationSubscribersByLocation {

    val locationId = LocationId()
    val subscribers = (1 to 3).map { _ => getEntity(locationId = locationId) }
    subscribers.foreach(service.save)
    (1 to 3).map { _ => getEntity() }.foreach(service.save)

    val locationSubscribers = service.findLocationSubscriptionsByLocation(locationId, OffsetLimit())
    locationSubscribers must haveSize(3)
    locationSubscribers must haveTheSameElementsAs(subscribers)

  }

  @Test
  def findLocationSubscribersByUser {

    val userId = UserId()
    val subscribers = (1 to 3).map { _ => getEntity(userId = userId) }
    subscribers.foreach(service.save)
    (1 to 3).map { _ => getEntity() }.foreach(service.save)

    val locationSubscribers = service.findLocationSubscriptionsByUser(userId, OffsetLimit())
    locationSubscribers must haveSize(3)
    locationSubscribers must haveTheSameElementsAs(subscribers)

  }

  @Test
  def countLocationSubscribers {

    val locationId = LocationId()
    (1 to 3).map { _ => getEntity(locationId = locationId) }.foreach(service.save)
    (1 to 3).map { _ => getEntity() }.foreach(service.save)

    service.countLocationSubscribers(locationId) === 3

  }


}
