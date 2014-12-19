package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.db.mongo.domain.{LocationsChainMedia, LocationsChain}
import com.tooe.core.domain._
import org.junit.Test
import com.tooe.core.util.DateHelper
import com.tooe.core.db.mongo.query.UpdateResult
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.LocationsChainId
import scala.Some

class LocationsChainDataServiceTest extends SpringDataMongoTestHelper {

  @Autowired var service: LocationsChainDataService = _
  lazy val entities = new MongoDaoHelper("locationschain")

  @Test
  def saveAndRead {
    val entity = new LocationsChainFixture().locationsChain
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def saveAndReadAll {
    val entities = (1 to 5).map(_ => new LocationsChainFixture().locationsChain).toSeq
    entities.foreach(service.save)
    val result = service.findAllById(entities.map(_.id))
    result.size === entities.size
    result.zip(entities).foreach{
      case (f,e) => f === e
    }
  }

  @Test
  def representation {
    val entity = new LocationsChainFixture().locationsChain
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "n" : ${entity.name.mongoRepr} ,
      "d" : ${entity.description.get.mongoRepr} ,
      "cid" : ${entity.companyId.id.mongoRepr} ,
      "t" : ${entity.registrationDate.mongoRepr} ,
      "lc" : ${entity.locationCount} ,
      "cm" : [ { "u" : { "mu" : "${entity.locationChainMedia.head.media.url.id}", "t" : "s3" } } ]
    }""")
  }

  @Test
  def changeLocationCounter {
    val entity = new LocationsChainFixture().locationsChain
    service.changeLocationCounter(entity.id, delta = 3) === UpdateResult.NotFound

    service.save(entity)
    service.changeLocationCounter(entity.id, delta = 2) === UpdateResult.Updated

    service.findOne(entity.id).get.locationCount === 2
  }

  @Test
  def updateMediaStorageToS3 {
    val media1 = new MediaObjectFixture(storage = UrlType.http).mediaObject
    val media2 = new MediaObjectFixture(storage = UrlType.http).mediaObject

    val entity = new LocationsChainFixture().locationsChain.copy(locationChainMedia = Seq(media1, media2).map(LocationsChainMedia))
    service.save(entity)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(entity.id, media2.url, expectedMedia.url)

    service.findOne(entity.id).map(_.locationChainMedia.map(_.media)) === Some(Seq(media1, expectedMedia))

  }

  @Test
  def updateMediaStorageToCDN {
    val media1 = new MediaObjectFixture().mediaObject
    val media2 = new MediaObjectFixture().mediaObject

    val entity = new LocationsChainFixture().locationsChain.copy(locationChainMedia = Seq(media1, media2).map(LocationsChainMedia))
    service.save(entity)

    service.updateMediaStorageToCDN(entity.id, media2.url)

    service.findOne(entity.id).map(_.locationChainMedia.map(_.media)) === Some(Seq(media1, media2.copy(mediaType = None)))

  }

}

class LocationsChainFixture {

  val locationsChain = LocationsChain(
    id = LocationsChainId(),
    name = Map("ru" -> "location-name"),
    description = Some(Map("ru" -> "location-description")),
    companyId = CompanyId(),
    registrationDate = DateHelper.currentDate,
    locationCount = 0,
    locationChainMedia = Seq(LocationsChainMedia(MediaObject("locationchain-media-url")))
  )
}