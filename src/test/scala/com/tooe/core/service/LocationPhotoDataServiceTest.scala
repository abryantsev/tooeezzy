package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain._
import java.util.Date
import com.tooe.core.domain._
import org.bson.types.ObjectId
import com.tooe.api.service.ChangeLocationPhotoRequest
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.domain.Unsetable.Update
import com.tooe.core.domain.LocationId
import scala.Some
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.LocationPhoto
import com.tooe.core.domain.MediaObjectId
import com.tooe.api.service.ChangeLocationPhotoRequest
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.HashHelper

class LocationPhotoDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationPhotoDataService = _

  lazy val entities = new MongoDaoHelper("location_photo")

  def getEntity(locationId: LocationId = LocationId(), photoAlbumId: LocationPhotoAlbumId = LocationPhotoAlbumId(), fileUrl: String = "url", creationDate: Date = new Date, likeCount: Int = 0) =
    LocationPhoto(locationId = locationId, fileUrl = MediaObject(MediaObjectId(fileUrl)), creationDate = creationDate, likesCount = likeCount, photoAlbumId = photoAlbumId)

  def generateUserIds(count: Int) = (1 to count).foldLeft(List.empty[UserId])((list, value) => list :+ UserId(new ObjectId()))

  def generateEntities(count: Int, locationId: LocationId = LocationId(), photoAlbumId: LocationPhotoAlbumId = LocationPhotoAlbumId(), fileUrl: String = "url", creationDate: Date = new Date, likeCount: Int = 0) =
    (1 to count).map {
      value =>
        getEntity(locationId, photoAlbumId, fileUrl, new Date(creationDate.getTime - value * 1000), likeCount)
    }

  def saveEntites(photos: Seq[LocationPhoto]) {
    photos.foreach(service.save)
  }

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = getEntity().copy(name = Some("location photo name"))
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : ${entity.id.id.mongoRepr} ,
      "pid" : ${entity.photoAlbumId.id.mongoRepr} ,
      "lid" : ${entity.locationId.id.mongoRepr} ,
      "n" : "${entity.name.getOrElse("")}",
      "u" : { "mu" : "${entity.fileUrl.url.id}", "t" : "s3" },
      "lc" : 0 ,
      "ls" : [ ${entity.usersLikesIds.map(_.id.mongoRepr).mkString(",")}  ],
      "cc" : 0 ,
      "cu" : [ ${entity.comments.map(_.id.mongoRepr).mkString(",")}  ],
      "t" : ${entity.creationDate.mongoRepr}
    }""")
  }

  @Test
  def getLocationPhotos() {
    val photos = (1 to 6).map(i => getEntity(locationId = LocationId(), fileUrl = i.toString,
      creationDate = new Date(2000 - 1900, i, 1)))
    photos.foreach(photo => service.save(photo))

    val result = service.getLocationPhotos(photos.map(_.id))

    result must haveSize(6)
    result mustEqual photos
  }

  @Test
  def updateUserLike {
    val entity = getEntity()
    service.save(entity)
    val fakeUserIds = generateUserIds(30)
    fakeUserIds.foreach {
      userId =>
        service.updateUserLikes(entity.id, userId)
        val currentEntity = service.findOne(entity.id).get
        currentEntity.usersLikesIds must contain(userId)
    }
    val currentEntity = service.findOne(entity.id).get
    currentEntity.likesCount === 30
    currentEntity.usersLikesIds must haveSize(20)
    currentEntity.usersLikesIds must haveTheSameElementsAs(fakeUserIds.drop(10))
  }

  @Test
  def updateUserLikes {
    val entity = getEntity(likeCount = 1)
    service.save(entity)
    val fakeUserIds = generateUserIds(10)

    service.updateUserLikes(entity.id, fakeUserIds)
    val currentEntity = service.findOne(entity.id).get
    currentEntity.usersLikesIds must haveSize(10)
    currentEntity.usersLikesIds must haveTheSameElementsAs(fakeUserIds.toSeq)
    currentEntity.likesCount === 0

  }

  @Test
  def addUserComment {
    val entity = getEntity()
    service.save(entity)
    val fakeUserIds = generateUserIds(30)
    fakeUserIds.foreach {
      userId =>
        service.addUserComment(entity.id, userId)
        val currentEntity = service.findOne(entity.id).get
        currentEntity.comments must contain(userId)
    }
    val currentEntity = service.findOne(entity.id).get
    currentEntity.commentsCount === 30
    currentEntity.comments must haveSize(20)
    currentEntity.comments must haveTheSameElementsAs(fakeUserIds.drop(10))
  }

  @Test
  def getLastLocationPhotos {
    val locationId = LocationId()
    val photos = generateEntities(count = 25, locationId = locationId)
    saveEntites(photos)

    val lastPhotos = service.getLastLocationPhotos(locationId)
    lastPhotos must haveSize(20)
    lastPhotos must haveTheSameElementsAs(photos.take(20))

  }

  @Test
  def getAllLocationPhotosByAlbum {
    val albumId = LocationPhotoAlbumId()
    val photos = generateEntities(count = 5, photoAlbumId = albumId)
    saveEntites(photos ++ generateEntities(count = 5))

    val albumsPhotos = service.getAllLocationPhotosByAlbum(albumId)
    albumsPhotos must haveSize(5)
    albumsPhotos must haveTheSameElementsAs(photos)
  }

  @Test
  def deletePhotosByAlbum {
    val locationId = LocationId()
    val albumId = LocationPhotoAlbumId()
    val albumsPhotos = generateEntities(count = 5, photoAlbumId = albumId, locationId = locationId)
    val locationsPhotos = generateEntities(count = 5, locationId = locationId)
    saveEntites(albumsPhotos ++ locationsPhotos)

    service.deletePhotosByAlbum(albumId)
    val locationsLastPhotos = service.getLastLocationPhotos(locationId)
    locationsLastPhotos must haveSize(5)
    locationsLastPhotos must haveTheSameElementsAs(locationsPhotos)
  }

  @Test
  def countByLocation {
    val locationId = LocationId()
    val albumsPhotos = generateEntities(count = 5, locationId = locationId)
    saveEntites(albumsPhotos ++ generateEntities(count = 5))

    service.countByLocation(locationId) === 5
  }

  @Test
  def changePhoto {
    val entity = new LocationPhotoFixture().locationPhoto
    service.save(entity)

    val newName = "new name"
    service.changePhoto(entity.id, ChangeLocationPhotoRequest(Update(newName)))

    service.findOne(entity.id).flatMap(_.name) === Some(newName)
  }

  @Test
  def updateMediaStorageToS3 {
    val photo = new LocationPhotoFixture().locationPhoto.copy(fileUrl = MediaObject(MediaObjectId(HashHelper.str("fileUrl")), UrlType.MigrationType))
    service.save(photo)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(photo.id, expectedMedia.url)

    service.findOne(photo.id).map(_.fileUrl) === Some(expectedMedia)
  }

  @Test
  def updateMediaStorageToCDN {
    val photo = new LocationPhotoFixture().locationPhoto
    service.save(photo)

    service.updateMediaStorageToCDN(photo.id)

    service.findOne(photo.id).flatMap(_.fileUrl.mediaType) === None
  }

}

class LocationPhotoFixture {

  val locationPhoto =
    LocationPhoto(locationId = LocationId(),
      name = Some("photo name"),
      fileUrl = MediaObject(MediaObjectId("file url")),
      creationDate = new Date,
      photoAlbumId = LocationPhotoAlbumId())

}