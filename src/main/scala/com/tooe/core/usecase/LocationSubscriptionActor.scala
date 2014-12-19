package com.tooe.core.usecase

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import akka.pattern._
import com.tooe.core.domain.{MediaUrl, LocationId, UserId}
import com.tooe.api.service.{OffsetLimit, SuccessfulResponse}
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor
import com.tooe.core.db.mongo.domain.{UserStatistics, Location, LocationSubscription}
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.location.LocationDataActor
import org.bson.types.ObjectId
import com.tooe.core.util.{Images, Lang}
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.exceptions.{ApplicationException, ForbiddenAppException, NotFoundException}

object LocationSubscriptionActor {
  final val Id = Actors.LocationSubscription

  case class AddLocationSubscription(userId: UserId, locationId: LocationId)

  case class DeleteLocationSubscription(userId: UserId, locationId: LocationId)

  case class GetLocationSubscription(locationId: LocationId, offsetLimit: OffsetLimit)

  case class GetUserLocationSubscriptions(userId: UserId, offsetLimit: OffsetLimit, lang: Lang)

}

class LocationSubscriptionActor extends Actor with AppActors with DefaultTimeout {

  import LocationSubscriptionActor._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val locationSubscriptionDataActor = lookup(LocationSubscriptionDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)

  def receive = {

    case AddLocationSubscription(userId, locationId) =>
      isLocationSubscriptionExists(userId, locationId).flatMap(exist => {
        if(!exist) {
          getLocation(locationId).map {
            case location if location.lifecycleStatusId.isEmpty =>
              locationSubscriptionDataActor ! CreateLocationSubscription(LocationSubscription(userId = userId, locationId = locationId))
              updateStatisticActor ! UpdateStatisticActor.UserChangeLocationSubscriptionsCounter(userId, 1)
              updateStatisticActor ! UpdateStatisticActor.LocationChangeLocationSubscriptionsCounter(locationId, 1)
              newsWriteActor ! NewsWriteActor.AddLocationSubscriptionsNews(userId, locationId)
              SuccessfulResponse
            case _ =>
              throw ApplicationException(message = s"Location is not active")
          }
        }
        else throw ApplicationException(message = s"Location subscription for locationId=${locationId.id.toString} already exists")
      }) pipeTo sender

    case DeleteLocationSubscription(userId, locationId) =>
      isLocationSubscriptionExists(userId, locationId).map(exist => {
        if(exist) {
          locationSubscriptionDataActor ! RemoveLocationSubscription(userId, locationId)
          updateStatisticActor ! UpdateStatisticActor.UserChangeLocationSubscriptionsCounter(userId, -1)
          updateStatisticActor ! UpdateStatisticActor.LocationChangeLocationSubscriptionsCounter(locationId, -1)
          SuccessfulResponse
        }
        else throw NotFoundException(s"Location subscription for locationId=${locationId.id.toString} not found")
      }) pipeTo sender

    case GetLocationSubscription(locationId, offsetLimit) =>
      (for {
        (subscription, count) <- (locationSubscriptionDataActor ? FindLocationSubscriptionsByLocation(locationId, offsetLimit))
          .zip(locationSubscriptionDataActor ? CountLocationSubscribers(locationId)).mapTo[(Seq[LocationSubscription], Long)]
        users <- (userReadActor ? UserReadActor.GetSubscriberStarsItemsDto(subscription.map(_.userId))).mapTo[Seq[SubscriberStarsItemDto]]
      } yield {
        LocationSubscriptionResponse(count, users.map(u => SubscriberStarsItem(u)))
      }) pipeTo sender

    case GetUserLocationSubscriptions(userId, offset, lang) =>
      implicit val l = lang
      val result = for {
        us <- getUserStatistics(userId)
        ls <- getLocationSubscriptionsByUser(userId, offset)
        locations <- getLocations(ls.map(_.locationId))
        responseItems = locations.map(GetUserLocationSubscriptionsResponseItem(lang))
      } yield GetUserLocationSubscriptionsResponse(us.locationSubscriptionsCount, responseItems)
      result.pipeTo(sender)

  }


  def isLocationSubscriptionExists(userId: UserId, locationId: LocationId) =  {
    locationSubscriptionDataActor.ask(LocationSubscriptionDataActor.ExistLocationSubscription(userId, locationId)).mapTo[Boolean]
  }

  def getLocationSubscriptionsByUser(userId: UserId, offset: OffsetLimit) =
    locationSubscriptionDataActor.ask(LocationSubscriptionDataActor.FindLocationSubscriptionsByUser(userId, offset)).mapTo[Seq[LocationSubscription]]

  def getLocations(ids: Seq[LocationId]) =
    locationDataActor.ask(LocationDataActor.FindLocations(ids.toSet)).mapTo[Seq[Location]]

  def getLocation(id: LocationId) =
    locationDataActor.ask(LocationDataActor.GetLocation(id)).mapTo[Location]

  def getUserStatistics(userId: UserId) =
    userDataActor.ask(UserDataActor.GetUserStatistics(userId)).mapTo[UserStatistics]

}

case class GetUserLocationSubscriptionsResponse(locationscount: Int, locations: Seq[GetUserLocationSubscriptionsResponseItem]) extends SuccessfulResponse

case class GetUserLocationSubscriptionsResponseItem(id: ObjectId, name: String, subscriberscount: Int, media: MediaUrl) extends UnmarshallerEntity

object GetUserLocationSubscriptionsResponseItem {
  def apply( lang: Lang)(l: Location): GetUserLocationSubscriptionsResponseItem =
    GetUserLocationSubscriptionsResponseItem(
      l.id.id,
      l.name.localized(lang).getOrElse(""),
      l.statistics.subscribersCount,
      l.getMainLocationMediaUrl(Images.Mylocationsubscriptions.Full.Location.Media)
    )
}

case class LocationSubscriptionResponse(@JsonProperty("userscount") count: Long, users: Seq[SubscriberStarsItem]) extends SuccessfulResponse
