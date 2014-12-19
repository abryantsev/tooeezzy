package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem, ActorRef}
import com.tooe.core.db.mongo.domain.{User, Region, Location, UserFixture}
import com.tooe.core.domain.{UserId, RegionId, LocationId}
import concurrent.Future
import com.tooe.core.service.{RegionFixture, LocationFixture}
import country.CountryDataActor
import region.RegionDataActor
import statistics.UpdateRegionOrCountryStatistic
import user.{UserDataActor, UpdateUserStatistic}
import com.tooe.core.usecase.location.{UpdateLocationStatistic, LocationDataActor}

class UpdateStatisticActorTest extends ActorTestSpecification {

  "UpdateStatisticActor" should {

    "send messages on add/remove favorite location" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val updater = UpdateRegionOrCountryStatistic(favorites = Some(1))
        val userUpdater = UpdateUserStatistic(favoriteLocations = Some(1))
        val locationUpdater = UpdateLocationStatistic(favoritePlacesCount = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeLocationFavoritesCounters(location.id, user.id, 1)
      regionProbe expectMsg RegionDataActor.UpdateStatistics(location.contact.address.regionId, updater)
      countryProbe expectMsg CountryDataActor.UpdateStatistics(location.contact.address.countryId, updater)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, userUpdater)
      locationProbe expectMsg LocationDataActor.UpdateStatistic(location.id, locationUpdater)
      success
    }

    "update users location subscriptions counter on ChangeLocationSubscriptionsCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.UserChangeLocationSubscriptionsCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(locationSubscriptions = Some(1)))
      success
    }

    "update users star subscriptions counter on ChangeUserStarSubscriptionsCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeUserStarSubscriptionsCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(userToStarSubscriptions = Some(1)))
      success
    }

    "update users friends counter on ChangeFriendshipCounters message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeFriendshipCounters(Set(user.id, friend.id), 1)
      userProbe expectMsg UserDataActor.UpdateStatisticForMany(Set(user.id, friend.id), UpdateUserStatistic(friends = Some(1)))
      success
    }
    "update users friends counter on ChangeFriendshipRequestCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeFriendshipRequestCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(friendshipRequests = Some(1)))
      success
    }
    "update users photoAlbums counter on ChangeUsersPhotoAlbumsCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeUsersPhotoAlbumsCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(photoAlbums = Some(1)))
      success
    }
    "update users wishes counter on ChangeUsersWishesCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeUsersWishesCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(wishes = Some(1)))
      success
    }
    "update users fulfilled wishes counter on ChangeUsersFulfilledWishesCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeUsersFulfilledWishesCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(fulfilledWishes = Some(1)))
      success
    }
    "update users events counters on UpdateUserEventCounters message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.UpdateUserEventCounters(user.id)
      userProbe.expectMsgAllOf(
        UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(userEvents = Some(user.statistics.newStatistic.eventsCount))),
        UserDataActor.SetStatistic(user.id, UpdateUserStatistic(newUserEvents = Some(0)))
      )
      success
    }
    "update users presents counter on SetUserNewPresentsCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.SetUserNewPresentsCounter(user.id, 0)
      userProbe.expectMsgAllOf(
        UserDataActor.SetStatistic(user.id, UpdateUserStatistic(newPresents = Some(0)))
      )
      success
    }
    "update location presents counter on ChangeLocationPresentCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeLocationPresentCounter(location.id, 1)
      locationProbe.expectMsgAllOf(
        LocationDataActor.UpdateStatistic(location.id, UpdateLocationStatistic(presentsCount = Some(1)))
      )
      success
    }
    "set counter on SetUserEventCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.SetUserEventCounter(user.id, 0)
      userProbe expectMsg UserDataActor.SetStatistic(user.id, UpdateUserStatistic(userEvents = Some(0)))
      success
    }
    "change users events counter on ChangeUserEventCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeUserEventCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(userEvents = Some(1)))
      success
    }
    "change users new events counter on ChangeNewUserEventCounter message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeNewUserEventCounter(user.id, 1)
      userProbe expectMsg UserDataActor.UpdateStatistic(user.id, UpdateUserStatistic(newUserEvents = Some(1)))
      success
    }
    "change users new events counters on ChangeNewUsersEventsCounters message" >> {
      val f = new UpdateStatisticActorFixture {}
      import f._

      val users = Set(user.id)
      updateStatisticActor ! UpdateStatisticActor.ChangeNewUsersEventsCounters(users, 1)
      userProbe expectMsg UserDataActor.UpdateStatisticForMany(users, UpdateUserStatistic(newUserEvents = Some(1)))
      success
    }
    "change users location counter on ChangeLocationsCounter message" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val updater = UpdateRegionOrCountryStatistic(locations = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeLocationsCounter(region.id, 1)
      regionProbe expectMsg RegionDataActor.UpdateStatistics(region.id, updater)
      countryProbe expectMsg CountryDataActor.UpdateStatistics(region.countryId, updater)
      success
    }
    "change users promotions counter on ChangePromotionsCounter message" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val updater = UpdateRegionOrCountryStatistic(promotions = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangePromotionsCounter(region.id, 1)
      regionProbe expectMsg RegionDataActor.UpdateStatistics(region.id, updater)
      countryProbe expectMsg CountryDataActor.UpdateStatistics(region.countryId, updater)
      success
    }
    "change users sales counter on ChangeSalesCounter message" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val updater = UpdateRegionOrCountryStatistic(sales = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeSalesCounter(location.id, 1)
      regionProbe expectMsg RegionDataActor.UpdateStatistics(location.contact.address.regionId, updater)
      countryProbe expectMsg CountryDataActor.UpdateStatistics(location.contact.address.countryId, updater)
      success
    }
    "change users counter on ChangeUsersCounter message" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val updater = UpdateRegionOrCountryStatistic(users = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeUsersCounter(location.contact.address.regionId, 1)
      regionProbe expectMsg RegionDataActor.UpdateStatistics(location.contact.address.regionId, updater)
      countryProbe expectMsg CountryDataActor.UpdateStatistics(region.countryId, updater)
      success
    }
    "change locations products counter on ChangeLocationProductsCounter message" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val locationUpdater = UpdateLocationStatistic(productsCount = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeLocationProductsCounter(location.id, 1)
      locationProbe expectMsg LocationDataActor.UpdateStatistic(location.id, locationUpdater)
      success
    }
    "change locations photoAlbums counter on ChangeLocationPhotoAlbumsCounter message" >> {
      val f = new UpdateStatisticActorCountryRegionFixture {
        val locationUpdater = UpdateLocationStatistic(photoAlbumsCount = Some(1))
      }
      import f._

      updateStatisticActor ! UpdateStatisticActor.ChangeLocationPhotoAlbumsCounter(location.id, 1)
      locationProbe expectMsg LocationDataActor.UpdateStatistic(location.id, locationUpdater)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class UpdateStatisticActorBaseFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user, friend = new UserFixture().user
  val location = new LocationFixture().entity
  val region = new RegionFixture().region

  def testActorFactory: UpdateStatisticActor

  lazy val updateStatisticActor = TestActorRef[UpdateStatisticActor](Props(testActorFactory))
}

abstract class UpdateStatisticActorFixture(implicit actorSystem: ActorSystem) extends UpdateStatisticActorBaseFixture {

  val userProbe, locationProbe = TestProbe()

  class UpdateStatisticActorUnderTest extends UpdateStatisticActor {
    override lazy val userDataActor: ActorRef = userProbe.ref
    override lazy val locationDataActor: ActorRef = locationProbe.ref

    override def getUser(userId: UserId): Future[User] = Future successful user

    override def getRegion(regionId: RegionId): Future[Region] = Future successful region

    override def getLocation(locationId: LocationId): Future[Location] = Future successful location
  }

  def testActorFactory = new UpdateStatisticActorUnderTest
}
abstract class UpdateStatisticActorCountryRegionFixture(implicit actorSystem: ActorSystem) extends UpdateStatisticActorBaseFixture {

  val regionProbe, countryProbe, userProbe, locationProbe = TestProbe()

  class UpdateStatisticActorUnderTest extends UpdateStatisticActor {
    override lazy val regionDataActor: ActorRef = regionProbe.ref
    override lazy val countryDataActor: ActorRef = countryProbe.ref
    override lazy val userDataActor: ActorRef = userProbe.ref
    override lazy val locationDataActor: ActorRef = locationProbe.ref

    override def getRegion(regionId: RegionId): Future[Region] = Future successful region

    override def getLocation(locationId: LocationId): Future[Location] = Future successful location
  }

  def testActorFactory = new UpdateStatisticActorUnderTest
}