package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.location.{LocationSearchViewType, LocationSearchSortType}
import com.tooe.core.domain._
import com.tooe.core.util.Lang
import com.tooe.core.usecase._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.CompanyId
import com.tooe.core.db.mongo.domain.AdditionalLocationCategory
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.usecase.LocationsSearchRequest
import scala.Some
import com.tooe.core.domain.AdditionalLocationCategoryId
import com.tooe.api.service.RouteContext
import com.tooe.core.db.mongo.domain.LocationContact
import com.tooe.core.domain.Coordinates
import com.tooe.core.usecase.ChangeAdditionalCategoryRequest
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.usecase.UpdateLocationRequest
import com.tooe.core.domain.RegionId
import com.tooe.core.db.mongo.domain.LocationCounters
import com.tooe.core.domain.LocationPhotoId
import com.tooe.core.db.mongo.domain.LocationAddress
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import com.mongodb.BasicDBObject
import com.tooe.core.domain.Unsetable.Skip

class LocationDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationDataService = _
  @Autowired var mongoTemplate: MongoTemplate = _

  lazy val entities = new MongoDaoHelper("location")

  @Test
  def saveAndReadAndDelete {
    val entity = defaultEntity.copy( hasPromotions = Some(true))
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.deleteLocation(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = defaultEntity.copy(lifecycleStatusId = Some(LifecycleStatusId.Deactivated))
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println(repr)
    jsonAssert(repr)( s"""{
       "_id" : { "$$oid" : "${entity.id.id.toString}" } ,
       "n" : { "ru" : "name"} ,
       "ns" : [ ${entity.names.mkString(",")} ] ,
       "cid" : ${entity.companyId.id.mongoRepr} ,
       "lcid" : ${entity.locationsChainId.get.id.mongoRepr} ,
       "d" : {  } ,
       "oh" : { } ,
       "c" : {
         "a" : {
          "l" : { "lon": 0.0 , "lat": 0.0},
          "rid" : { "$$oid" : "${entity.contact.address.regionId.id.toString}"},
          "r" : "regionName",
          "cid" : "cid",
          "co" : "country",
          "s" : "street"
          } ,
         "p" : [ ]
       } ,
       "lc" : [] ,
       "lac" : [
                {
                  "cid": { "$$oid" : "${entity.additionalLocationCategories.head.id.id.toString}"},
                  "n" : { "orig" : "name" }
                }
               ] ,
       "pa" : [] ,
       "lm" : [] ,
       "lp" : [],
       "uf" : [],
       "st" : {
            "prc" : 0,
            "pac" : 0,
            "rc" : 0,
            "sc" : 0,
            "fc" : 0,
            "pc" : 0,
            "cc" : 0
       },
       "lfs" : ${entity.lifecycleStatusId.get.id},
       "lsr" : "tooeezzy"
     }""")
  }

  @Test
  def findInRadius {
    val coordinates = (37.625587, 55.74073)
    val radius = 3000
    val location = Location(
      companyId = CompanyId(),
      locationsChainId = None,
      contact = LocationContact(address = LocationAddress(
        coordinates = Coordinates(37.629587, 55.74973),
        regionId = RegionId(new ObjectId()),
        regionName = "regionName",
        country = "country",
        countryId = CountryId("cid"),
        street = "street"
      )),
      lifecycleStatusId = None
    )
    service.find(coordinates, radius) must not contain (location)
    service.save(location)
    service.find(coordinates, radius) must contain (location)
  }

  import com.tooe.core.util.SomeWrapper._

  @Test
  def locationExistenceCheck() {
    val entity = defaultEntity
    val existingId = entity.id
    val nonExistingId = LocationId()

    service.save(entity)

    service.isLocationExist(existingId) === true
    service.isLocationExist(nonExistingId) === false
  }

  @Test
  def updateLocation() {
    val entity = defaultEntity
    service.findOne(entity.id) === None
    service.save(entity) === entity
    val preModerated = new PreModerationLocationFixture().entity.copy(lifecycleStatusId = None)

    service.updateLocation(entity.id, preModerated)

    val updatedEntity = service.findOne(entity.id).get
    updatedEntity.name === preModerated.name
    updatedEntity.companyId === preModerated.companyId
    updatedEntity.locationsChainId === preModerated.locationsChainId
    updatedEntity.description === preModerated.description
    updatedEntity.openingHours === preModerated.openingHours
    updatedEntity.locationCategories === preModerated.locationCategories
    updatedEntity.additionalLocationCategories === preModerated.additionalLocationCategories
    updatedEntity.locationMedia === preModerated.locationMedia
    updatedEntity.lifecycleStatusId === preModerated.lifecycleStatusId

  }

  @Test
  def addPhotoToLocation {
    val location = defaultEntity
    service.save(location)
    val photosIds = generateIds(10).map(id => LocationPhotoId(id))
    photosIds.foreach(photoId => service.addPhotoToLocation(location.id, photoId))

    val locationsPhotos = service.findOne(location.id).map(_.lastPhotos).getOrElse(Nil)

    locationsPhotos must haveSize(6)
    locationsPhotos must haveTheSameElementsAs(photosIds.drop(4))

  }

  @Test
  def getLocations {
    val location = defaultEntity
    service.getLocations(Seq(location.id)) === Seq()
    service.save(location)
    service.getLocations(Seq(location.id)) === Seq(location)
  }

  @Test
  def getActiveLocationOrByLifeCycleStatuses {
    val location = defaultEntity.copy(lifecycleStatusId = Some(LifecycleStatusId.Deactivated))
    service.getActiveLocationOrByLifeCycleStatuses(location.id, Seq(LifecycleStatusId.Deactivated)) === None
    service.save(location)
    service.getActiveLocationOrByLifeCycleStatuses(location.id, Seq(LifecycleStatusId.Deactivated)) === Some(location)
  }

  @Test
  def updateLocationPhotos {
    val location = defaultEntity
    service.save(location)
    val photoIds = generateIds(20).map(LocationPhotoId(_))
    service.updateLocationPhotos(location.id, photoIds)
    val updatedLocation = service.findOne(location.id)
    val lastPhotos = updatedLocation.map(_.lastPhotos).getOrElse(Nil)
    lastPhotos must haveSize(6)
    lastPhotos.forall(photoIds.toSet) === true
  }

  @Test
  def addOwnProductCategory {
    val location = defaultEntity
    service.save(location)
    val categoryId = AdditionalLocationCategoryId()
    val categoryName = "categoryName"
    val ctx = RouteContext("v01", "ru")
    implicit val lang = ctx.lang
    service.addOwnCategory(location.id, categoryId, categoryName)
    service.findOne(location.id).flatMap(_.additionalLocationCategories.find(_.name.localized == Some(categoryName))).isDefined === true
  }

  @Test
  def removeOwnProductCategory {
    val f = new LocationFixture
    import f._
    service.save(entity)
    val categoryId = AdditionalLocationCategoryId()
    val categoryName = "categoryName"
    val ctx = RouteContext("v01", "ru")
    implicit val lang = ctx.lang
    def isCategoryExist(category: String): Boolean = {
      service.findOne(entity.id).flatMap(_.additionalLocationCategories.find(_.name.localized == Some(category))).isDefined
    }

    isCategoryExist(categoryName) === false
    service.addOwnCategory(entity.id, categoryId, categoryName)
    isCategoryExist(categoryName) === true
    service.removeAdditionalCategory(entity.id, categoryId)
    isCategoryExist(categoryName) === false

  }

  @Test
  def getStatistics {
    val counter = LocationCounters(presentsCount = 1, photoalbumsCount = 2, reviewsCount = 3, subscribersCount = 4)
    val entity = defaultEntity.copy(statistics = counter)
    service.save(entity)
    service.getStatistics(entity.id) === Some(counter)
  }

  @Test
  def renameAdditionalCategory {
    implicit val lang = Lang.orig
    val entity = defaultEntity.copy(
      additionalLocationCategories = Seq(AdditionalLocationCategory(id = AdditionalLocationCategoryId(), name = Map("orig" -> "name", "ru" -> "rus name")),
                                        AdditionalLocationCategory(id = AdditionalLocationCategoryId(), name = Map("orig" -> "not change name", "ru" -> "rus not change name"))))
    service.save(entity)
    service.renameAdditionalCategory(ChangeAdditionalCategoryRequest(entity.id, entity.additionalLocationCategories.head.id),
      RenameAdditionalCategoryParameters("new name"),
      lang)

    val changedEntity = service.findOne(entity.id)
    changedEntity.flatMap(_.additionalLocationCategories.head.name.localized) === Some("new name")
    changedEntity.flatMap(_.additionalLocationCategories.head.name.localized(Lang.ru)) === Some("rus name")
    changedEntity.flatMap(_.additionalLocationCategories.reverse.head.name.localized) === Some("not change name")
    changedEntity.flatMap(_.additionalLocationCategories.reverse.head.name.localized(Lang.ru)) === Some("rus not change name")

  }

  @Test
  def putToAndRemoveFromUsersWhoFavorite {
    val userId = UserId()
    val entity = defaultEntity.copy( hasPromotions = Some(true))
    service.findOne(entity.id) === None
    service.save(entity) === entity
    
    service.putUserToUsersWhoFavorite(entity.locationId, userId)
    service.findOne(entity.id).flatMap(_.usersWhoFavorite) === Some(Seq(userId))

    service.removeUserFromUsersWhoFavorite(entity.locationId, userId)
    service.findOne(entity.id).flatMap(_.usersWhoFavorite) === Some(Seq())

  }

  @Test
  def searchLocationsByCoordinatesOrderedByDistance {
    
    val f = new LocationGeoSearchFixture()
    import f._
    val locationsSearchRequest = request.copy(sort = LocationSearchSortType.Distance)
    def findLocationsWithDistances = service.getLocationsSearchByCoordinates(locationsSearchRequest, lang).locationsWithDistance

    findLocationsWithDistances must not contain projectedLocationWithDistance
    service.save(location)

    findLocationsWithDistances.map(_.location) must contain (projectedLocationWithDistance.location)

    service.save(location2)
    findLocationsWithDistances.map(_.location.id) === Seq(location, location2).map(_.id)
  }

  @Test
  def searchLocationsByCoordinatesOrderedByName {

    val f = new LocationGeoSearchFixture()
    import f._
    val locationsSearchRequest = request.copy(sort = LocationSearchSortType.Name)
    def findLocationsWithDistances = service.getLocationsSearchByCoordinates(locationsSearchRequest, lang).locationsWithDistance

    findLocationsWithDistances must not contain projectedLocationWithDistance
    service.save(location)

    findLocationsWithDistances.map(_.copy(distance = None)) must contain (projectedLocationWithDistance)

    service.save(location2)
    findLocationsWithDistances.map(_.location.id) === Seq(location, location2).map(_.id)

  }

  @Test
  def getLocationsForInvitation {
    val f = new LocationFixture()
    import f._
    val friendId = UserId()
    val location = entity.copy(usersWhoFavorite = Seq(friendId))
    val request = GetLocationsForInvitationRequest(
      region = location.contact.address.regionId,
      category = location.locationCategories.headOption,
      name = location.name.localized(Lang.ru),
      userIds = Option(Set(friendId)),
      sort = None,
      entitiesParam = locationLocationCountViewType,
      offsetLimit = OffsetLimit(0, 20)
    )
    service.save(location)
    val getLocations = service.getLocationsForInvitation(request, Lang.ru)
    getLocations.locations === Seq(location)
    getLocations.locationsCount === Some(1)
  }
  @Test
  def getLocationsForCheckin {
    entities.collection.remove(new BasicDBObject())
    val f = new LocationFixture()
    import f._
    val request = GetLocationsForCheckinRequest(longitude = 0.0, latitude = 0.0,
      entitiesParam = locationLocationCountViewType,
      offsetLimit = OffsetLimit(0, 20)
    )
    service.save(entity)
    val getLocations = service.getLocationsForCheckin(request, Lang.ru)
    getLocations.locations === Seq(entity)
    getLocations.locationsCount === Some(1)
  }

  @Test
  def setPromotionsFlag {
    val f = new LocationFixture()
    import f._
    service.save(entity)
    service.setPromotionsFlag(entity.id, Some(true))
    service.findOne(entity.id).get.hasPromotions === Some(true)
  }

  @Test
  def addLocationsToChain {
    val f = new LocationFixture()
    import f._
    service.save(entity)
    val locationChainId = LocationsChainId()
    service.addLocationsToChain(locationChainId, Seq(entity.id))
    service.findOne(entity.id).get.locationsChainId === Some(locationChainId)
  }

  @Test
  def getMedia {
    val locationMedia = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val location = new LocationFixture().entity.copy(locationMedia = Seq(locationMedia).map(mo => LocationMedia(mo)))
    service.save(location)

    service.getMedia(location.id) === Some(Seq(locationMedia.url))
  }

  @Test
  def updateMediaStorageToS3 {
    val locationMedia1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val locationMedia2 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val location = new LocationFixture().entity.copy(locationMedia = Seq(locationMedia1, locationMedia2).map(mo => LocationMedia(mo)))
    service.save(location)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(location.id, locationMedia2.url, expectedMedia.url)

    service.findOne(location.id).map(_.locationMedia.map(_.url)) === Some(Seq(locationMedia1, expectedMedia))
  }

  @Test
  def updateMediaStorageToCDN {
    val locationMedia1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val locationMedia2 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val location = new LocationFixture().entity.copy(locationMedia = Seq(locationMedia1, locationMedia2).map(mo => LocationMedia(mo)))
    service.save(location)

    service.updateMediaStorageToCDN(location.id, locationMedia2.url)

    service.findOne(location.id).map(_.locationMedia.map(_.url)) === Some(Seq(locationMedia1, locationMedia2.copy(mediaType = None)))
  }

  @Test
  def updateLifecycleStatus() {
    val entity = service.save(defaultEntity.copy(lifecycleStatusId = None))
    service.updateLifecycleStatus(entity.id, Some(LifecycleStatusId.Deactivated))
    service.getLocationForAdmin(entity.id).flatMap(_.lifecycleStatusId) === Some(LifecycleStatusId.Deactivated)
    service.updateLifecycleStatus(entity.id, None)
    service.getLocationForAdmin(entity.id).flatMap(_.lifecycleStatusId) === None
  }

  def generateIds(count: Int) = (1 to count).map(value => new ObjectId)

  def defaultEntity = new LocationFixture().entity
}

@Document(collection = "location") case class LocationNameSearch(_id: ObjectId, ns: java.util.List[String])


class LocationFixture {

  val entity = Location(
    companyId = CompanyId(),
    locationsChainId = Some(LocationsChainId()),
    name = Map("ru" -> "name"),
    description = ObjectMap.empty,
    openingHours = ObjectMap.empty,
    contact = locationContact(),
    locationCategories = Nil,
    hasPromotions = None,
    locationMedia = Nil,
    photoAlbums = Nil,
    additionalLocationCategories = Seq(AdditionalLocationCategory(id = AdditionalLocationCategoryId(), name = Map("orig" -> "name"))),
    usersWhoFavorite = Nil,
    lifecycleStatusId = None,
    specialRole = Some(LocationSpecialRole("tooeezzy"))
  )
  def locationContact(coords: Coordinates = Coordinates(0.0, 0.0)) = LocationContact(
    address = LocationAddress(
      coordinates = coords,
      regionId = RegionId(),
      regionName = "regionName",
      countryId = CountryId("cid"),
      country = "country",
      street = "street"
    ),
    phones = Nil,
    url = None
  )
  val updateLocationRequest = UpdateLocationRequest(None, None, None, None, None, None, None, None, None, Skip)
  val locationLocationCountViewType= Some(Set(LocationSearchViewType.Locations, LocationSearchViewType.LocationsCount))
}

class LocationGeoSearchFixture {

  import com.tooe.core.util.SomeWrapper._

  implicit val lang = Lang.ru
  val f = new LocationFixture()
  import f._
  val lid = LocationId()
  val location = entity.copy(locationCategories = Seq(LocationCategoryId()), hasPromotions = true, name = Map("ru" -> "Bbbb"), usersWhoFavorite = Seq(UserId()))
  val location2 = location.copy(id = lid,name = Map("ru" -> "Bbbbcbef"), contact = locationContact(Coordinates(0.01, 0.01)), statistics = LocationCounters(subscribersCount = 10))

  val projectedLocation = location.copy(
    additionalLocationCategories = Nil,
    companyId = null,
    usersWhoFavorite = Nil,
    lifecycleStatusId = None,
    locationsChainId = None,
    specialRole = None
  )

  val request = {
    LocationsSearchRequest(
      category = location.locationCategories.head,
      radius = None,
      longitude = 0.0,
      latitude = 0.0,
      sort = LocationSearchSortType.Name,
      entitiesParam = locationLocationCountViewType,
      offsetLimit = OffsetLimit(0, 20)
    )
  }
  val projectedLocationWithDistance = LocationWithDistance(projectedLocation, None)


}
