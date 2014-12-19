package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.HashHelper
import com.tooe.core.domain._
import org.bson.types.ObjectId
import scala.annotation.tailrec
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.UserId

class CheckinDataServiceTest extends SpringDataMongoTestHelper{
  @Autowired var service: CheckinDataServiceImpl = _

  lazy val entities = new MongoDaoHelper("check_in")

  @Test
  def saveAndRead {
    val entity = new CheckinFixture().checkin
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def searchNearOrderedByDistance {
    val f = new SearchNearFixture()
    import f._
    checkins foreach service.save

    def searchIds(offsetLimit: OffsetLimit) =
      service.searchNearOrderedByDistance(searchNearRequest.copy(offsetLimit = offsetLimit), radiusM) map (_.checkin.id)

    searchIds(OffsetLimit(0, 10)) === checkinsIdsOrderedByDistance
    searchIds(OffsetLimit(1, 1)) === checkinsIdsOrderedByDistance.slice(1, 2)
  }

  @Test
  def searchNearOrderedByDistanceCheckDistance {
    val f = new SearchNearFixture()
    import f._
    checkins foreach service.save

    def search(offsetLimit: OffsetLimit) =
      service.searchNearOrderedByDistance(searchNearRequest.copy(offsetLimit = offsetLimit), radiusM)

    search(OffsetLimit(0, 10)).forall(_.checkin.asInstanceOf[Checkin].friends === Nil) === true

    @tailrec
    def increasing(elems: List[Double], result: Boolean = true): Boolean = elems match {
      case a :: b :: tail if result => increasing(b :: tail, a < b)
      case _                        => result
    }

    val distances = search(OffsetLimit(0, 10)) flatMap (_.distance)
    increasing(distances.toList) === true
  }

  @Test
  def searchNearCount {
    val f = new SearchNearFixture()
    import f._
    checkins foreach service.save

    service.searchNearCount(searchNearRequest, radiusM) === 3
  }

  @Test
  def searchNearOrderedByName {
    val f = new SearchNearFixture()
    import f._
    checkins foreach service.save

    def search(offsetLimit: OffsetLimit) =
      service.searchNearOrderedByName(searchNearRequest.copy(offsetLimit = offsetLimit), radiusM)

    def searchIds(offsetLimit: OffsetLimit) = search(offsetLimit) map (_.checkin.id)

    search(OffsetLimit(0, 10)).forall(_.checkin.asInstanceOf[Checkin].friends === Nil) === true

    searchIds(OffsetLimit(0, 10)) === checkinsIdsOrderedByNames
    searchIds(OffsetLimit(1, 1)) === checkinsIdsOrderedByNames.slice(1, 2)
  }

  @Test
  def representation {
    val entity = doCheckin()
    val repr = entities.findOne(entity.id.id)
    println(repr.toString)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
      "t" : ${entity.creationTime.mongoRepr} ,
      "u" : {
        "uid" : { "$$oid" : "${entity.user.userId.id.toString}"} ,
        "n" : "${entity.user.name}",
        "ln" : "${entity.user.lastName}",
        "sn" : "${entity.user.secondName.get}",
        "m" : { "mu" : "${entity.user.media.map(_.url.id).getOrElse("")}", "t" : "s3" },
        "g" : "${entity.user.gender.id}"
      } ,
      "lo" : {
        "lid" : { "$$oid" : "${entity.location.locationId.id.toString}"},
        "l" : { "lon": 0.0 , "lat": 0.0},
        "oh" : "${entity.location.openingHours}",
        "n" : "${entity.location.name}" ,
        "a" : {
          "co": "${entity.location.address.country}" ,
          "r": "${entity.location.address.region}",
          "s" : "${entity.location.address.street}"
        } ,
        "m" : { "mu" : "${entity.location.media.map(_.url.id).getOrElse("")}" , "t" : "s3" }
      },
      fs: []
    }""")
  }

  @Test
  def findCheckedInUsersByLocation {
    val f = new FindCheckedInUsersFixture()
    import f._

    checkins.foreach (service.save(_))

    service.findCheckedInUsers(locationId, user1, OffsetLimit(0, 5)) === Seq(user2, user3, user4)
    service.findCheckedInUsers(locationId, user1, OffsetLimit(1, 1)) === Seq(user3)
  }

  @Test
  def countCheckedInUsersByLocation {
    val f = new FindCheckedInUsersFixture()
    import f._

    checkins.foreach (service.save(_))

    service.countCheckedInUsers(locationId, user1) === 3
  }

  @Test
  def findCheckedInFriendsByLocation {
    val f = new FindCheckedInUsersFixture()
    import f._

    checkins.foreach (service.save(_))

    service.findCheckedInFriends(locationId, user1, OffsetLimit(0, 5)) === Seq(user2, user3)
    service.findCheckedInFriends(locationId, user1, OffsetLimit(1, 2)) === Seq(user3)
  }

  @Test
  def countCheckedInFriendsByLocation {
    val f = new FindCheckedInUsersFixture()
    import f._

    checkins.foreach (service.save(_))

    service.countCheckedInFriends(locationId, user1) === 2
  }

  class FindCheckedInUsersFixture {
    val user1, user2, user3, user4 = UserId(new ObjectId())
    val locationId = LocationId(new ObjectId())
    
    val checkIn1 = defaultCheckin(userId = user1, locationId = locationId)
    val checkIn2 = defaultCheckin(userId = user2, locationId = locationId, friendsIds = Seq(user1)) 
    val checkIn3 = defaultCheckin(userId = user3, locationId = locationId, friendsIds = Seq(user1)) 
    val checkIn4 = defaultCheckin(userId = user4, locationId = locationId)
    
    val checkins = Seq(checkIn1, checkIn2, checkIn3, checkIn4)
  }

  @Test
  def findCheckedIntoLocationUsers {
    val user1, user2, user3, user4 = UserId(new ObjectId())
    val location1, location2 = LocationId(new ObjectId())
    doCheckin(user1, location1)
    doCheckin(user2, location1)
    doCheckin(user3, location2)
    doCheckin(user4, location1, hoursBeforeExpire = -3)
    service.findCheckedInUsers(Seq(user1, user3), location1) === Seq(user1)
  }

  def doCheckin(userId: UserId = UserId(),
                locationId: LocationId = LocationId(),
                name: String = HashHelper.str("name"),
                lastName: String = HashHelper.str("lastName"),
                hoursBeforeExpire: Int = 3, coordinates: Coordinates = Coordinates(), friendsIds: Seq[UserId] = Seq()) = {
    val checkin = defaultCheckin(userId, name, lastName, locationId, hoursBeforeExpire, coordinates, friendsIds)
    service.save(checkin)
  }

  def defaultCheckin(userId: UserId = UserId(),
                     name: String = HashHelper.str("name"),
                     lastName: String = HashHelper.str("lastName"),
                     locationId: LocationId = LocationId(),
                     hoursBeforeExpire: Int = 3,
                     coordinates: Coordinates = Coordinates(),
                     friendsIds: Seq[UserId] = Seq()) = {
    Checkin(
      user = UserInfoCheckin(
        userId = userId,
        name = name,
        secondName = Some("secondName"),
        lastName = lastName,
        media = Some(MediaObject("media")),
        gender = Gender.Male
      ),
      location = LocationInfoCheckin(
        locationId = locationId,
        coordinates = coordinates,
        openingHours = "hours",
        name = "locationName",
        address = LocationAddressItem("country", "region", "street"),
        media = Some(MediaObject("locationMedia"))
      ),
      friends = friendsIds
    )
  }

  @Test
  def changeUrlTypeToCdn {

    val checkin = new CheckinFixture().checkin
    service.save(checkin)

    service.changeUrlTypeToCdn(Urls(entityType = EntityType.checkinLocation,
                                    entityId = checkin.id.id,
                                    mediaId = checkin.location.media.map(_.url).getOrElse(MediaObjectId(""))))

    service.findOne(checkin.id).get.location.media.get.mediaType === None

    service.changeUrlTypeToCdn(Urls(entityType = EntityType.checkinUser,
                                    entityId = checkin.id.id,
                                    mediaId = checkin.location.media.map(_.url).getOrElse(MediaObjectId(""))))

    service.findOne(checkin.id).get.user.media.get.mediaType === None

  }
}

class CheckinFixture(coords: Coordinates = Coordinates(), name: String = "name", lastName: String = "lastName", userId: UserId = UserId()) {
  val checkin = Checkin(
    user = UserInfoCheckin(
      userId = userId,
      name = name,
      secondName = Some("secondName"),
      lastName = lastName,
      media = Some(MediaObject("media")),
      gender = Gender.Male
    ),
    location = LocationInfoCheckin(
      locationId = LocationId(),
      coordinates = coords,
      openingHours = "hours",
      name = "locationName",
      address = LocationAddressItem("country", "region", "street"),
      media = Some(MediaObject("locationMedia"))
    ),
    friends = Seq(UserId())
  )
}

class SearchNearFixture  {
  val radiusM = 4000

  val userId = UserId()

  val coords = new CoordinatesFixture().coords
  val coords2 = coords.copy(longitude = coords.longitude + 0.001, latitude = coords.latitude - 0.001)
  val coords3 = coords.copy(longitude = coords.longitude - 0.0012, latitude = coords.latitude + 0.0012)
  val coordsTooFar = coords.copy(longitude = coords.longitude - 0.1, latitude = coords.latitude + 0.1)

  val searchNearRequest = SearchNearParams(coords, OffsetLimit(0, 10), userId)

  val usersCheckin = new CheckinFixture(coords = coords, name = "0000", lastName = "0000", userId = userId).checkin
  val checkin1 = new CheckinFixture(coords = coords, name = "Cccc", lastName = "Abc").checkin
  val checkin2 = new CheckinFixture(coords = coords2, name = "Aaaa", lastName = "Abc").checkin
  val checkin3 = new CheckinFixture(coords = coords3, name = "Aaaa", lastName = "Bbb").checkin
  val tooFarCheckin = new CheckinFixture(coords = coordsTooFar, name = "Aaaa", lastName = "Bbb").checkin

  val checkinsShouldBeExcluded = Seq(tooFarCheckin, usersCheckin)
  val checkins = checkinsShouldBeExcluded ++ Seq(checkin3, checkin1, checkin2)
  val checkinsIdsOrderedByNames = Seq(checkin2.id, checkin3.id, checkin1.id)
  val checkinsIdsOrderedByDistance = Seq(checkin1.id, checkin2.id, checkin3.id)
}