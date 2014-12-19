package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import org.junit.Test
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.usecase.{ModerationLocationsSearchRequest, UpdateLocationAddressItem, UpdateLocationRequest}
import com.tooe.core.util.HashHelper.uuid
import org.bson.types.ObjectId
import com.tooe.api.service.LocationModerationRequest
import com.tooe.core.util.Lang
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.Unsetable.Update


class PreModerationLocationDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: PreModerationLocationDataService = _

  lazy val entities = new MongoDaoHelper("location_mod")

  @Test
  def saveAndRead() {
    val entity = new PreModerationLocationFixture().entity
    service.save(entity)

    service.findById(entity.id) === Some(entity)
  }

  @Test
  def findByLocationId() {
    val entity = new PreModerationLocationFixture().entity
    service.save(entity)

    service.findByLocationId(entity.publishedLocation.get) === Some(entity)
  }

  @Test
  def representation() {
    val entity = new PreModerationLocationFixture().entity
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println(repr)
    jsonAssert(repr)( s"""{
       "_id" : ${entity.id.id.mongoRepr},
       "cid" : ${entity.companyId.id.mongoRepr} ,
       "lcid" : ${entity.locationsChainId.get.id.mongoRepr} ,
       "n" : { "ru" : "name"} ,
       "ns" : [ ${entity.names.mkString(",")}] ,
       "d" : {  } ,
       "oh" : { } ,
       "c" : {
         "a" : {
          "l" : { "lon": 0.0 , "lat": 0.0},
          "rid" : ${entity.contact.address.regionId.id.mongoRepr},
          "r" : "${entity.contact.address.regionName}",
          "co" : "${entity.contact.address.country}",
          "cid" : "${entity.contact.address.countryId.id}",
          "s" : "${entity.contact.address.street}"
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
       "lm" : [ { "u" : { "mu" : "${entity.locationMedia.head.url.url.id}" , "t" : "s3" }, "t" : "" } ]  ,
       "lfs" : ${entity.lifecycleStatusId.get.id},
       "puid" : ${entity.publishedLocation.get.id.mongoRepr},
       "mod": {
                  "s" : "${entity.moderationStatus.status.id}",
                  "m" : "${entity.moderationStatus.message.get}",
                  "uid" : ${entity.moderationStatus.adminUser.get.id.mongoRepr}
              }
     }""")
  }

  @Test
  def findOwnModerationLocations {
    import com.tooe.core.util.SomeWrapper._

    val companyId = CompanyId()
    val moderationStatusId = ModerationStatusId("status")
    val locations = generatePreModerationLocations(moderationStatusId, companyId)
    locations.foreach(service.save)

    val request = ModerationLocationsSearchRequest("COOL NAME", moderationStatusId, None, OffsetLimit())

    val found = service.findOwnModerationLocations(request, Set(companyId), Lang.ru)

    found.size === locations.size

    found.zip(locations).foreach {
      case (f, e) => f === e
    }
  }

  @Test
  def countOwnModerationLocations() {
    import com.tooe.core.util.SomeWrapper._

    val companyId = CompanyId()
    val moderationStatusId = ModerationStatusId("status")
    val locations = generatePreModerationLocations(moderationStatusId, companyId)
    locations.foreach(service.save)

    val request = ModerationLocationsSearchRequest("COOL NAME", moderationStatusId, None, OffsetLimit())

    val count = service.countOwnModerationLocations(request, Set(companyId), Lang.ru)

    count === locations.size
  }

  @Test
  def findByAdminSearchParams() {
    import com.tooe.core.util.SomeWrapper._

    val companyId = CompanyId()
    val moderationStatusId = ModerationStatusId("status")
    val locations = generatePreModerationLocations(moderationStatusId, companyId)
    locations.foreach(service.save)

    val params = PreModerationLocationAdminSearchParams("COOL NAME", companyId, moderationStatusId, None, OffsetLimit(), Lang.ru)

    val found = service.findByAdminSearchParams(params)

    found.size === locations.size

    found.zip(locations).foreach {
      case (f, e) => f === e
    }
  }

  @Test
  def countByAdminSearchParams() {
    import com.tooe.core.util.SomeWrapper._

    val companyId = CompanyId()
    val moderationStatusId = ModerationStatusId("status")
    val locations = generatePreModerationLocations(moderationStatusId, companyId)
    locations.foreach(service.save)

    val params = PreModerationLocationAdminSearchParams("COOL NAME", companyId, moderationStatusId, None, OffsetLimit(), Lang.ru)

    val count = service.countByAdminSearchParams(params)

    count === locations.size
  }

  @Test
  def updateLocation {
    val entity = new PreModerationLocationFixture().entity
    service.findById(entity.id) === None
    service.save(entity) === entity

    val countryCode = uuid
    val url = uuid

    val ulr = UpdateLocationRequest(
        name = Some(uuid),
        openingHours = Some(uuid),
        description = Some(uuid),
        address = Some(UpdateLocationAddressItem(Some(RegionId(new ObjectId())), Some(uuid))),
        categories = Some(Seq(LocationCategoryId(uuid))),
        coordinates = Some(Coordinates()),
        phone = Some(uuid),
        countryCode = Some(countryCode),
        media = Some(MediaUrl("new url")),
        url = Update(url)
    )

    val lang = Lang.ru

    service.updateLocation(entity.id, ulr, lang)

    val updatedEntity = entity.copy(
        name = Map(lang -> ulr.name.get),
        openingHours = Map(lang -> ulr.openingHours.get),
        description = Map(lang -> ulr.description.get),
        contact = LocationContact(address = LocationAddress(
            coordinates = ulr.coordinates.get,
            regionId = ulr.address.get.regionId.get,
            countryId = CountryId("cid"),
            street = ulr.address.get.street.get,
            regionName = "regionName",
            country = "country"
          ),
          phones = ulr.phone.map(p => Phone(number = p, countryCode = countryCode, purpose = Some("main"))).toSeq,
          url = Some(url)
        ),
        locationCategories = ulr.categories.get,
        locationMedia = Seq(LocationMedia(url = MediaObject(ulr.media.map(_.imageUrl).getOrElse("")), purpose = Some("main")))
      )
    service.findById(entity.id) ===  Some(updatedEntity)
  }

  @Test
  def getPublishLocationId {
    val entity = new PreModerationLocationFixture().entity
    service.findById(entity.id) === None
    service.save(entity) === entity

    service.getPublishLocationId(entity.id) === entity.publishedLocation
  }

  @Test
  def updatePublishId {
    val entity = new PreModerationLocationFixture().entity
    service.findById(entity.id) === None
    service.save(entity) === entity

    val publishId = LocationId()

    service.updatePublishId(entity.id, publishId)

    service.getPublishLocationId(entity.id) === Some(publishId)

  }

  @Test
  def updateModerationStatus {
    val entity = new PreModerationLocationFixture().entity
    service.findById(entity.id) === None
    service.save(entity) === entity

    val moderationStatus = LocationModerationRequest(ModerationStatusId.Active, Some("moderation message"))
    val adminId = AdminUserId()

    service.updateModerationStatus(entity.id, moderationStatus, adminId)

    val moderatedEntityStatus = service.findById(entity.id).map(_.moderationStatus)
    moderatedEntityStatus.map(ms => LocationModerationRequest(ms.status, ms.message)) === Some(moderationStatus)
    moderatedEntityStatus.flatMap(_.adminUser) === Some(adminId)

  }

  @Test
  def updateMediaStorageToS3 {
    val locationMedia1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val locationMedia2 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val location = new PreModerationLocationFixture().entity.copy(locationMedia = Seq(locationMedia1, locationMedia2).map(mo => LocationMedia(mo)))
    service.save(location)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(location.id, locationMedia2.url, expectedMedia.url)

    service.findById(location.id).map(_.locationMedia.map(_.url)) === Some(Seq(locationMedia1, expectedMedia))
  }

  @Test
  def updateMediaStorageToCDN {
    val locationMedia1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val locationMedia2 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val location = new PreModerationLocationFixture().entity.copy(locationMedia = Seq(locationMedia1, locationMedia2).map(mo => LocationMedia(mo)))
    service.save(location)

    service.updateMediaStorageToCDN(location.id, locationMedia2.url)

    service.findById(location.id).map(_.locationMedia.map(_.url)) === Some(Seq(locationMedia1, locationMedia2.copy(mediaType = None)))
  }

  private def generatePreModerationLocations(moderationStatusId: ModerationStatusId, companyId: CompanyId) = {
    import com.tooe.core.util.SomeWrapper._
    val name = "Cool Name"
    (1 to 5).map(i => new PreModerationLocationFixture().entity
      .copy(companyId = companyId, name = Map(Lang.ru -> name.concat(i.toString)), moderationStatus = PreModerationStatus(
      status = moderationStatusId,
      message = "hello",
      adminUser = AdminUserId()
    ))).toSeq
  }

  @Test
  def updateLifecycleStatus() {
    val entity = service.save(new PreModerationLocationFixture().entity.copy(lifecycleStatusId = None))
    service.updateLifecycleStatus(entity.id, Some(LifecycleStatusId.Deactivated))
    service.findById(entity.id).flatMap(_.lifecycleStatusId) === Some(LifecycleStatusId.Deactivated)
    service.updateLifecycleStatus(entity.id, None)
    service.findById(entity.id).flatMap(_.lifecycleStatusId) === None
  }


}


class PreModerationLocationFixture {

  import com.tooe.core.util.SomeWrapper._

  val locationFixture = new LocationFixture

  val entity = PreModerationLocation(
    companyId = CompanyId(),
    locationsChainId = LocationsChainId(),
    name = Map("ru" -> "name"),
    description = ObjectMap.empty,
    openingHours = ObjectMap.empty,
    contact = locationFixture.locationContact(),
    locationCategories = Nil,
    locationMedia = Seq(LocationMedia(MediaObject("some_url"))),
    additionalLocationCategories = Seq(AdditionalLocationCategory(id = AdditionalLocationCategoryId(), name = Map("orig" -> "name"))),
    lifecycleStatusId = LifecycleStatusId.Deactivated,
    publishedLocation = LocationId(),
    moderationStatus = PreModerationStatus(
      message = "hello",
      adminUser = AdminUserId()
    )
  )
}
