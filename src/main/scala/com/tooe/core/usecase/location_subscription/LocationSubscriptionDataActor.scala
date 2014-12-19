package com.tooe.core.usecase.location_subscription

import akka.actor.Actor
import akka.pattern._
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.domain.LocationSubscription
import com.tooe.core.infrastructure.BeanLookup
import scala.concurrent.Future
import com.tooe.core.service.LocationSubscriptionDataService
import com.tooe.core.domain.{LocationId, UserId}
import com.tooe.api.service.OffsetLimit

object LocationSubscriptionDataActor {
  final val Id = Actors.LocationSubscriptionData

  case class CreateLocationSubscription(locationSubscription: LocationSubscription)
  case class RemoveLocationSubscription(userId: UserId, locationId: LocationId)
  case class ExistLocationSubscription(userId: UserId, locationId: LocationId)
  case class FindLocationSubscriptionsByLocation(locationId: LocationId, offsetLimit: OffsetLimit = OffsetLimit())
  case class FindLocationSubscriptionsByUser(userId: UserId, offsetLimit: OffsetLimit)
  case class CountLocationSubscribers(locationId: LocationId)
}

class LocationSubscriptionDataActor extends Actor with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationSubscriptionDataActor._

  lazy val service = BeanLookup[LocationSubscriptionDataService]

  def receive = {

    case CreateLocationSubscription(locationSubscription) => Future { service.save(locationSubscription) }
    case RemoveLocationSubscription(userId, locationId) => Future { service.remove(userId, locationId) }
    case ExistLocationSubscription(userId, locationId) => Future { service.existSubscription(userId, locationId) } pipeTo sender
    case FindLocationSubscriptionsByLocation(locationId, offsetLimit) => Future { service.findLocationSubscriptionsByLocation(locationId, offsetLimit) } pipeTo sender
    case FindLocationSubscriptionsByUser(userId, offsetLimit) => Future { service.findLocationSubscriptionsByUser(userId, offsetLimit) } pipeTo sender
    case CountLocationSubscribers(locationId) => Future { service.countLocationSubscribers(locationId) } pipeTo sender
  }

}
