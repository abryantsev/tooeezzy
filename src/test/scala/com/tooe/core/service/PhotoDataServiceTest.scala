package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.{PhotoLike, Photo}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.bson.types.ObjectId
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.util.HashHelper
import com.tooe.api.service.{PhotoChangeRequest, OffsetLimit}
import com.tooe.core.db.mongo.domain.PhotoLike
import com.tooe.api.service.PhotoChangeRequest
import scala.Some
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.domain.UserId
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.domain.Unsetable.Update

class PhotoDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PhotoDataService = _
  @Autowired var likeService: PhotoLikeDataService = _

  lazy val entities = new MongoDaoHelper("photo")

  def getEntity(name: String = "", fileUrl:String = "", userId: UserId = UserId(), photoAlbumId: PhotoAlbumId = PhotoAlbumId() ) =
    Photo(
      name = Some(name),
      fileUrl = MediaObject(MediaObjectId(fileUrl)),
      userId = userId,
      photoAlbumId = photoAlbumId
    )


  @Test
  def saveAndRead {
    val entity = getEntity("test", "photo url")
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = getEntity()
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id}" } ,
      "uid" : ${entity.userId.id.mongoRepr} ,
      "pa" : ${entity.photoAlbumId.id.mongoRepr} ,
      "n" : "${entity.name.getOrElse("")}",
      "u" : { "mu" : "${entity.fileUrl.url.id}", "t" : "s3" },
      "lc" : 0 ,
      "ls" : [ ${entity.usersLikesIds.map(_.id.mongoRepr).mkString(",")}  ],
      "cc" : 0 ,
      "cu" : [ ${entity.usersCommentsIds.map(_.id.mongoRepr).mkString(",")}  ],
      "t"  : ${entity.createdAt.mongoRepr}
    }""")
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
  def removePhotos {

    val firstAlbumId = PhotoAlbumId(new ObjectId)
    val secondAlbumId = PhotoAlbumId(new ObjectId)
    val photoInFirstAlbum1 = getEntity(photoAlbumId = firstAlbumId)
    service.save(photoInFirstAlbum1)
    val photoInFirstAlbum2 = getEntity(photoAlbumId = firstAlbumId)
    service.save(photoInFirstAlbum2)
    val photoInSecondAlbum1 = getEntity(photoAlbumId = secondAlbumId)
    service.save(photoInSecondAlbum1)
    service.removePhotos(firstAlbumId)
    service.findOne(photoInFirstAlbum1.id) === None
    service.findOne(photoInFirstAlbum2.id) === None
    service.findOne(photoInSecondAlbum1.id) === Some(photoInSecondAlbum1)

  }

  @Test
  def findByAlbum {
    val albumId = PhotoAlbumId()
    val photo1 = new PhotoFixture().photo.copy(createdAt = new Date(System.currentTimeMillis()), photoAlbumId = albumId)
    val photo2 = new PhotoFixture().photo.copy(createdAt = new Date(System.currentTimeMillis() - 1000), photoAlbumId = albumId)
    val photo3 = new PhotoFixture().photo.copy(createdAt = new Date(System.currentTimeMillis() - 2000))

    Seq(photo1, photo2, photo3).foreach(service.save)

    service.findByAlbumId(albumId, OffsetLimit(None, Some(Int.MaxValue))) === Seq(photo1, photo2)

  }

  @Test
  def getLastUserPhotos {
    val userId = UserId()
    val photo1 = new PhotoFixture().photo.copy(createdAt = new Date(System.currentTimeMillis()), userId = userId)
    val photo2 = new PhotoFixture().photo.copy(createdAt = new Date(System.currentTimeMillis() - 1000),  userId = userId)
    val photo3 = new PhotoFixture().photo.copy(createdAt = new Date(System.currentTimeMillis() - 2000))

    Seq(photo1, photo2, photo3).foreach(service.save)

    service.getLastUserPhotos(userId) === Seq(photo1, photo2)
  }

  @Test
  def countPhotosInAlbum {
    val firstAlbumId = PhotoAlbumId(new ObjectId)
    val secondAlbumId = PhotoAlbumId(new ObjectId)
    val photoInFirstAlbum1 = getEntity(photoAlbumId = firstAlbumId)
    service.save(photoInFirstAlbum1)
    val photoInFirstAlbum2 = getEntity(photoAlbumId = firstAlbumId)
    service.save(photoInFirstAlbum2)
    val photoInSecondAlbum1 = getEntity(photoAlbumId = secondAlbumId)
    service.save(photoInSecondAlbum1)
    service.countPhotosInAlbum(firstAlbumId) === 2
    service.countPhotosInAlbum(secondAlbumId) === 1
  }

  @Test
  def updateUserLikes {
    val entity = getEntity()
    service.save(entity)
    val fakeUserIds = (1 to 30).map(_ => UserId())
    fakeUserIds.foreach { userId =>
      service.updateUserLikes(entity.id, userId)
      val currentEntity = service.findOne(entity.id).get
      currentEntity.usersLikesIds must contain(userId)
    }
    val currentEntity = service.findOne(entity.id).get
    currentEntity.likesCount === 30
    currentEntity.usersLikesIds must haveSize(20)
    currentEntity.usersLikesIds must haveTheSameElementsAs(fakeUserIds.drop(10).toSeq)
  }

  @Test
  def updateUserDislikes {
    val entity = getEntity()
    service.save(entity)
    val fakeUserIds = (1 to 30).map(_ => UserId()).toList
    fakeUserIds.foreach { userId =>
      service.updateUserLikes(entity.id, userId)
      likeService.save(new PhotoLike(photoId = entity.id, userId = userId, time = new Date))
    }

    val userStillLikes = fakeUserIds.drop(1).take(20)
    service.updateUserDislikes(entity.id, userStillLikes)

    val currentEntity = service.findOne(entity.id).get
    currentEntity.likesCount === 29
    currentEntity.usersLikesIds === userStillLikes
  }

  @Test
  def updatePhoto {
    val photo = new PhotoFixture().photo
    service.save(photo)
    val updateRequest = PhotoChangeRequest(name = Update(HashHelper.str("photoName")))
    service.updatePhoto(photo.id, updateRequest)
    val updatedPhoto = service.findOne(photo.id)

    updatedPhoto.flatMap(_.name).get === updateRequest.name.get
    updatedPhoto.flatMap(_.name) !== photo.name

  }

  @Test
  def updateMediaStorageToS3 {
    val photo = new PhotoFixture().photo.copy(fileUrl = MediaObject(MediaObjectId(HashHelper.str("fileUrl")), UrlType.MigrationType))
    service.save(photo)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(photo.id, expectedMedia.url)

    service.findOne(photo.id).map(_.fileUrl) === Some(expectedMedia)
  }

  @Test
  def updateMediaStorageToCDN {
    val photo = new PhotoFixture().photo
    service.save(photo)

    service.updateMediaStorageToCDN(photo.id)

    service.findOne(photo.id).flatMap(_.fileUrl.mediaType) === None
  }

}

class PhotoFixture {
  val photo = Photo(
    photoAlbumId = PhotoAlbumId(),
    userId = UserId(),
    name = Some(HashHelper.str("photoName")),
    fileUrl = MediaObject(MediaObjectId(HashHelper.str("fileUrl")))
  )
}
