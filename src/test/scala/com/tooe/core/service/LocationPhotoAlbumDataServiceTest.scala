package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import scala.Some
import com.tooe.core.domain._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.usecase.location_photoalbum.UpdateLocationPhotoAlbumRequest
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.util.HashHelper
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.usecase.location_photoalbum.UpdateLocationPhotoAlbumRequest
import scala.Some
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.location_photoalbum.UpdateLocationPhotoAlbumRequest
import scala.Some
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.LocationsChainId

class LocationPhotoAlbumDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationPhotoAlbumDataServiceImpl = _
  lazy val entities = new MongoDaoHelper("location_photoalbum")

  def getEntity(name: String = "name", locationId: LocationId = LocationId(), description: Option[String] = Some("description"), photosCount: Int = 0, frontPhotoUrl: String = "front photo", locationChainsId: LocationsChainId = LocationsChainId()) =
    LocationPhotoAlbum(
      name = name,
      locationId = locationId,
      description = description,
      photosCount = photosCount,
      frontPhotoUrl = MediaObject(MediaObjectId(frontPhotoUrl)),
      locationsChainId = Some(locationChainsId)
    )

  def generateEntity(count: Int, name: String = "name", locationId: LocationId = LocationId(), description: Option[String] = Some("description"), photosCount: Int = 0, frontPhotoUrl: String = "fron photo") =
    (1 to count) map {
      value =>
        getEntity(name, locationId, description, photosCount, frontPhotoUrl)
    }

  def saveEntities(entities: Seq[LocationPhotoAlbum]) = entities.foreach(service.save)

  @Test
  def findOne {
    val locationPhotoAlbum = getEntity()

    service.save(locationPhotoAlbum) === locationPhotoAlbum
    service.findOne(locationPhotoAlbum.id) === Some(locationPhotoAlbum)
  }

  @Test
  def delete {
    val entity = getEntity()
    service.save(entity)
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def representation {
    val entity = getEntity().copy(
      photosCount = 14
    )
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "lid" : ${entity.locationId.id.mongoRepr} ,
      "n" : "${entity.name}",
      "d" : "${entity.description.getOrElse("")}",
      "c" : 14,
      "p" : { "mu" : "${entity.frontPhotoUrl.url.id}", "t" : "s3" } ,
      "lcid" : ${entity.locationsChainId.get.id.mongoRepr}
    }""")
  }

  @Test
  def countByLocation {
    val locationId = LocationId()
    val locationPhotos = generateEntity(count = 5, locationId = locationId)
    saveEntities(locationPhotos ++ generateEntity(5))

    service.countByLocation(locationId) === 5

  }

  @Test
  def albumsByLocation {
    val locationId = LocationId()
    val locationPhotos = generateEntity(count = 5, locationId = locationId)
    saveEntities(locationPhotos ++ generateEntity(5))

    val fetchedAlbums = service.albumsByLocation(locationId, OffsetLimit(Some(0), Some(15)))
    fetchedAlbums must haveSize(5)
    fetchedAlbums must haveTheSameElementsAs(locationPhotos)
  }

  @Test
  def updatePhotosCount {
    val entity = getEntity(photosCount = 5)
    service.save(entity)
    service.updatePhotosCount(entity.id, 1)
    service.findOne(entity.id).map(_.photosCount) === Some(6)
    service.updatePhotosCount(entity.id, -1)
    service.findOne(entity.id).map(_.photosCount) === Some(5)
  }

  @Test
  def update {
    val entity = getEntity()
    service.save(entity)

    val request = UpdateLocationPhotoAlbumRequest(name = Some("new name"), description = Update("new description"), photoUrl = Some(MediaObject(MediaObjectId("new url"), None)))
    service.update(entity.id, request)

    val updatedEntity = service.findOne(entity.id)

    updatedEntity.map(_.name) === request.name
    updatedEntity.flatMap(_.description) === Some(request.description.get)
    updatedEntity.map(_.frontPhotoUrl) === request.photoUrl

  }

  @Test
  def countChainsAlbums {
    val locationsChainId = LocationsChainId()
    val albums = (1 to 3).map { _ => getEntity(locationChainsId = locationsChainId) }
    albums.foreach(service.save)
    (1 to 3).map { _ => getEntity() }.foreach(service.save)

    val chainsAlbums = service.findChainsAlbums(locationsChainId, OffsetLimit())
    chainsAlbums must haveSize(3)
    chainsAlbums must haveTheSameElementsAs(albums)

  }

  @Test
  def findChainsAlbums {
    val locationsChainId = LocationsChainId()
    val albums = (1 to 3).map { _ => getEntity(locationChainsId = locationsChainId) }
    albums.foreach(service.save)
    (1 to 3).map { _ => getEntity() }.foreach(service.save)

    service.countChainsAlbums(locationsChainId) === 3
  }

  @Test
  def updateMediaStorageToS3 {
    val album = new LocationPhotoAlbumFixture(mediaType = UrlType.MigrationType).album
    service.save(album)
    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(album.id, expectedMedia.url)

    service.findOne(album.id).map(_.frontPhotoUrl) === Some(expectedMedia)
  }

  @Test
  def updateMediaStorageToCDN {
    val album = new LocationPhotoAlbumFixture(mediaType = Some(UrlType.s3)).album
    service.save(album)

    service.updateMediaStorageToCDN(album.id)

    service.findOne(album.id).flatMap(_.frontPhotoUrl.mediaType) === None
  }

}

class LocationPhotoAlbumFixture(url: String = HashHelper.str("url"), mediaType: Option[UrlType] = Some(UrlType.s3)) {

  val album = LocationPhotoAlbum(
    name = HashHelper.str("name"),
    locationId = LocationId(),
    description = Some("description"),
    photosCount = 0,
    frontPhotoUrl = MediaObject(MediaObjectId(url), mediaType),
    locationsChainId = Some(LocationsChainId())
  )

}