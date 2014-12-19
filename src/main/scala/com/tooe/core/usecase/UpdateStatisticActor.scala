package com.tooe.core.usecase

import country.CountryDataActor
import com.tooe.core.usecase.location.{UpdateLocationStatistic, LocationDataActor}
import region.RegionDataActor
import scala.concurrent.Future
import com.tooe.core.usecase.statistics.{ArrayOperation, LocationCategoriesUpdate, UpdateRegionOrCountryStatistic}
import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.{User, Region, Location}
import user.{UpdateUserStatistic, UserDataActor}
import com.tooe.api.service.ExecutionContextProvider
import com.tooe.core.db.mongo.domain.Region
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.domain.LocationId
import scala.Some
import com.tooe.core.domain.RegionId
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.User
import com.tooe.core.usecase.user.UpdateUserStatistic
import com.tooe.core.domain.CountryId

object UpdateStatisticActor{
  final val Id = Actors.UpdateStatistic

  case class ChangeUsersCounter(regionId: RegionId, delta: Int )
  case class ChangeLocationsCounter(regionId: RegionId, delta: Int, country: Option[CountryId] = None)
  case class ChangeUsersPhotoAlbumsCounter(userId: UserId, delta: Int )
  case class ChangeUsersWishesCounter(userId: UserId, delta: Int )
  case class ChangeUsersFulfilledWishesCounter(userId: UserId, delta: Int )
  case class UserChangeLocationSubscriptionsCounter(userId: UserId, delta: Int)
  case class LocationChangeLocationSubscriptionsCounter(location: LocationId, delta: Int)
  case class ChangeUserStarSubscriptionsCounter(userId: UserId, delta: Int)
  case class ChangeStarSubscriptionsCounter(userId: UserId, delta: Int)
  case class ChangePromotionsCounter(regionId: RegionId, delta: Int )
  case class ChangeLocationFavoritesCounters(locationId: LocationId, userId: UserId, delta: Int)
  case class ChangeSalesCounter(locationId: LocationId, delta: Int)
  case class ChangeFriendshipRequestCounter(userId: UserId, delta: Int)
  case class ChangeFriendshipCounters(userIds: Set[UserId], delta: Int)

  case class SetUserEventCounter(userId: UserId, delta: Int)
  case class ChangeUserEventCounter(userId: UserId, delta: Int)
  case class ChangeNewUserEventCounter(userId: UserId, delta: Int)
  case class ChangeNewUsersEventsCounters(userIds: Set[UserId], delta: Int)
  case class UpdateUserEventCounters(userId: UserId)
  case class SetUserNewPresentsCounter(userId: UserId, delta: Int)
  case class ChangeUserNewPresentsCounter(userId: UserId, delta: Int)
  case class ChangeUserPresentsCounter(userId: UserId, delta: Int)
  case class ChangeUserCertificatesCounter(userId: UserId, delta: Int)
  case class ChangeUserSentPresentsCounter(userId: UserId, delta: Int)
  case class ChangeUserSentCertificatesCounter(userId: UserId, delta: Int)

  case class ChangeLocationPresentCounter(locationId: LocationId, delta: Int)
  case class ChangeLocationProductsCounter(locationId: LocationId, delta: Int)
  case class ChangeLocationPhotoAlbumsCounter(locationId: LocationId, delta: Int)

  case class AddLocationCategories(region: RegionId, categories: Seq[LocationCategoryId], country: CountryId)
  case class RemoveLocationCategories(region: RegionId, categories: Seq[LocationCategoryId], country: CountryId)
}

class UpdateStatisticActor extends AppActor with ExecutionContextProvider {

  lazy val regionDataActor = lookup(RegionDataActor.Id)
  lazy val countryDataActor = lookup(CountryDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)

  import UpdateStatisticActor._

  def receive = {
    case ChangeUsersPhotoAlbumsCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(photoAlbums = Some(delta)))
    case ChangeUsersFulfilledWishesCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(fulfilledWishes = Some(delta)))
    case ChangeUsersWishesCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(wishes = Some(delta)))
    case ChangeFriendshipCounters(userIds, delta) =>
      userDataActor ! UserDataActor.UpdateStatisticForMany(userIds, UpdateUserStatistic(friends = Some(delta)))
    case ChangeUserStarSubscriptionsCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(userToStarSubscriptions = Some(delta)))
    case ChangeFriendshipRequestCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(friendshipRequests = Some(delta)))
    case UpdateUserEventCounters(userId) =>
      getUser(userId) map  { user =>
        userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(userEvents = Some(user.statistics.newStatistic.eventsCount)))
        userDataActor ! UserDataActor.SetStatistic(userId, UpdateUserStatistic(newUserEvents = Some(0)))
      }
    case SetUserNewPresentsCounter(userId, delta) =>
      userDataActor ! UserDataActor.SetStatistic(userId, UpdateUserStatistic(newPresents = Some(delta)))
    case ChangeUserNewPresentsCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(newPresents = Some(delta)))
    case ChangeUserPresentsCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(presents = Some(delta)))
    case ChangeUserCertificatesCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(certificates = Some(delta)))
    case ChangeUserSentPresentsCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(sentPresents = Some(delta)))
    case ChangeUserSentCertificatesCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(sentCertificates = Some(delta)))
    case SetUserEventCounter(userId, delta) =>
      userDataActor ! UserDataActor.SetStatistic(userId, UpdateUserStatistic(userEvents = Some(delta)))
    case ChangeUserEventCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(userEvents = Some(delta)))
    case ChangeNewUserEventCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(newUserEvents = Some(delta)))
    case ChangeNewUsersEventsCounters(userIds, delta) =>
      userDataActor ! UserDataActor.UpdateStatisticForMany(userIds, UpdateUserStatistic(newUserEvents = Some(delta)))

    case ChangeLocationsCounter(regionId, delta, country) =>
      updateRegionAndCountryStatistic(regionId, UpdateRegionOrCountryStatistic(locations = Some(delta)), country)

    case UserChangeLocationSubscriptionsCounter(userId, delta) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(locationSubscriptions = Some(delta)))

    case ChangeLocationPresentCounter(locationId, delta) =>
      locationDataActor ! LocationDataActor.UpdateStatistic(locationId, UpdateLocationStatistic(presentsCount = Some(delta)))

    case LocationChangeLocationSubscriptionsCounter(location, delta) =>
      locationDataActor ! LocationDataActor.UpdateStatistic(location, UpdateLocationStatistic(subscribersCount = Some(delta)))
    case ChangeLocationProductsCounter(locationId, delta) =>
      locationDataActor ! LocationDataActor.UpdateStatistic(locationId, UpdateLocationStatistic(productsCount = Some(delta)))
      getLocation(locationId).map { location =>
        updateRegionAndCountryStatistic(location.contact.address.regionId,
          UpdateRegionOrCountryStatistic(products = Some(delta)), country = Some(location.contact.address.countryId))
      }
    case ChangeLocationPhotoAlbumsCounter(locationId, delta) =>
      locationDataActor ! LocationDataActor.UpdateStatistic(locationId, UpdateLocationStatistic(photoAlbumsCount = Some(delta)))

    case ChangeLocationFavoritesCounters(locationId, userId, delta) =>
      getLocation(locationId).foreach { location =>
        locationDataActor ! LocationDataActor.UpdateStatistic(locationId, UpdateLocationStatistic(favoritePlacesCount = Some(delta)))
        updateRegionAndCountryStatistic(location.contact.address.regionId,
          UpdateRegionOrCountryStatistic(favorites = Some(delta)), country = Some(location.contact.address.countryId))
      }
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(favoriteLocations = Some(delta)))

    case ChangePromotionsCounter(regionId, delta) =>
      updateRegionAndCountryStatistic(regionId, UpdateRegionOrCountryStatistic(promotions = Some(1)))

    case ChangeSalesCounter(locationId, delta) =>
      getLocation(locationId).map { location =>
        updateRegionAndCountryStatistic(location.contact.address.regionId,
          UpdateRegionOrCountryStatistic(sales = Some(delta)), country = Some(location.contact.address.countryId))
      }
    case ChangeUsersCounter(regionId, delta) =>
      updateRegionAndCountryStatistic(regionId, UpdateRegionOrCountryStatistic(users = Some(1)))

    case ChangeStarSubscriptionsCounter(userId: UserId, delta: Int) =>
      userDataActor ! UserDataActor.UpdateStatistic(userId, UpdateUserStatistic(newUserEvents = Some(delta)))

    case AddLocationCategories(region, cats, country) =>
      val update = cats.map(cat => UpdateRegionOrCountryStatistic(
        locationCategoriesUpdate = Some(LocationCategoriesUpdate(cat,
          ArrayOperation.PushToSet))))
      update.foreach(u => updateRegionAndCountryStatistic(region, u, Some(country)))

    case RemoveLocationCategories(region, cats, country) =>
      val update = cats.map(cat => UpdateRegionOrCountryStatistic(
        locationCategoriesUpdate = Some(LocationCategoriesUpdate(cat,
          ArrayOperation.Delete))))
      update.foreach(u => updateRegionAndCountryStatistic(region, u, Some(country)))
  }

  def updateRegionAndCountryStatistic(regionId: RegionId, updater: UpdateRegionOrCountryStatistic, country: Option[CountryId] = None) {
    regionDataActor ! RegionDataActor.UpdateStatistics(regionId, updater)
    if (country.isDefined) { countryDataActor ! CountryDataActor.UpdateStatistics(country.get, updater)
    } else getRegion(regionId).map(region => countryDataActor ! CountryDataActor.UpdateStatistics(region.countryId, updater))
  }

  def getRegion(regionId: RegionId): Future[Region] = {
    regionDataActor.ask(RegionDataActor.GetRegion(regionId)).mapTo[Region]
  }

  def getLocation(locationId: LocationId): Future[Location] = {
    (locationDataActor ? LocationDataActor.GetLocation(locationId)).mapTo[Location]
  }
  def getUser(userId: UserId): Future[User] = {
    (userDataActor ? UserDataActor.GetUser(userId)).mapTo[User]
  }
}
