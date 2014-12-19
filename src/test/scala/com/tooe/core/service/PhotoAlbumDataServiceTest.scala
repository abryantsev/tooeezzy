package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.PhotoAlbum
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.api.service.{OffsetLimit, UserGroups}
import com.tooe.core.usecase.PhotoAlbumWriteActor.EditPhotoAlbumFields
import com.tooe.core.domain.{UrlType, MediaObject, MediaObjectId, UserId}
import org.bson.types.ObjectId
import com.tooe.core.util.HashHelper
import java.util.Date

class PhotoAlbumDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PhotoAlbumDataService = _

  lazy val entities = new MongoDaoHelper("photoalbum")

  def getEntity(name: String = "name", description: Option[String] = Some("description"), fontPhotoUrl: String = "front photo", allowedView: Seq[String] = Seq(), allowedComment: Seq[String] = Seq(), userId: UserId = UserId(), count: Int = 0, time: Date = new Date) =
    PhotoAlbum(
      name = name,
      description = description,
      frontPhotoUrl = MediaObject(MediaObjectId(fontPhotoUrl)),
      allowedView = allowedView,
      allowedComment = allowedComment,
      userId = userId,
      count = count
    )

  def generateEntities(count: Int, name: String = "name", description: Option[String] = Some("description"), fontPhotoUrl: String = "front photo", allowedView: Seq[String] = Seq(), allowedComment: Seq[String] = Seq(), userId: UserId = UserId()) =
    (1 to count).map {
      index =>
        getEntity(name, description, fontPhotoUrl, allowedView, allowedComment, userId, 0, new Date(System.currentTimeMillis - index * 100000))
    }

  def saveEntities(photoAlbums: Seq[PhotoAlbum]) {
    photoAlbums foreach {
      photoAlbum => service.save(photoAlbum)
    }
  }

  @Test
  def saveAndRead {
    val entity = getEntity(
      allowedView = Nil //Seq("Family", "Friends")
    )
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def representation {
    val entity = getEntity(allowedView = Seq("Family", "Friends"), allowedComment = Seq("Bestfriend", "Collegue")).copy(default = Some(true))
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr)( s"""{
      "_id" : { "$$oid" : "${entity.id.id}" } ,
      "uid" : ${entity.userId.id.mongoRepr} ,
      "c" : 0 ,
      "av" : [ "Family" , "Friends"],
      "ac" : [ "Bestfriend" , "Collegue"],
      "n" : "${entity.name}",
      "d" : "${entity.description.getOrElse("")}",
      "p" : { "mu" : "${entity.frontPhotoUrl.url.id}", "t" : "s3" },
      "t" : ${entity.createdTime.mongoRepr},
      "de" : true
    }""")
  }

  @Test
  def includeAllowedUserGroups {
    val entity = getEntity(allowedView = Seq("Family"))
    service.save(entity)
    service.includeUserGroups(entity.id, "Colleges")
    val found = service.findOne(entity.id).get
    found.allowedView.toSet === Set("Family", "Colleges")
  }

  @Test
  def excludeAllowedUserGroups {
    val entity = getEntity(
      allowedView = Seq("Family")
    )
    service.save(entity)
    service.excludeUserGroups(entity.id, "Family")
    val found = service.findOne(entity.id).get
    found.allowedView must (beEmpty)

    val repr = entities.findOne(entity.id.id)
    println("repr=" + repr)
    jsonAssert(repr, strict = false)( s"""{
      "av" : []
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
  def update {

    val entity = getEntity()

    service.save(entity)

    val changedResultEntity = PhotoAlbum(
      id = entity.id,
      userId = entity.userId,
      name = "changed name",
      description = Some("changed description"),
      frontPhotoUrl = MediaObject(MediaObjectId("changed front photo")),
      allowedView = Seq("friends"),
      allowedComment = Seq("family"),
      createdTime = entity.createdTime
    )

    service.update(entity.id, EditPhotoAlbumFields(
      name = Some(changedResultEntity.name),
      description = changedResultEntity.description,
      photoUrl = Some(MediaObject(changedResultEntity.frontPhotoUrl.url.id)),
      usergroups = Some(UserGroups(view = Some(changedResultEntity.allowedView), comments = Some(changedResultEntity.allowedComment)))
    ))

    val result = service.findOne(entity.id)
    result === Some(changedResultEntity)

  }

  @Test
  def emptyUpdate {

    val entity = getEntity()

    service.save(entity)

    service.update(entity.id, EditPhotoAlbumFields(
      name = None,
      description = None,
      photoUrl = None,
      usergroups = None
    ))

    val result = service.findOne(entity.id)
    result === Some(entity)

  }

  @Test
  def changePhotoCount {
    val entity = getEntity(count = 5)
    service.save(entity)
    service.changePhotoCount(entity.id, 3)
    service.findOne(entity.id).map(_.count) === Some(8)
    service.changePhotoCount(entity.id, -5)
    service.findOne(entity.id).map(_.count) === Some(3)
  }

  @Test
  def findPhotoAlbumsByUser {
    val authorId = UserId()
    val myAlbums = generateEntities(5, userId = authorId)
    val otherAlbums = generateEntities(5)
    saveEntities(myAlbums)
    saveEntities(otherAlbums)
    val searchResult = service.findPhotoAlbumsByUser(authorId, OffsetLimit(None, Some(10)))
    searchResult must haveSize(5)
    searchResult === myAlbums.sortWith((l: PhotoAlbum, r: PhotoAlbum) => l.createdTime.getTime > r.createdTime.getTime)

  }

  @Test
  def updateMediaStorageToS3 {
    val album = new PhotoAlbumFixture().photoAlbum.copy(frontPhotoUrl = MediaObject(MediaObjectId(HashHelper.str("photoUrl")), UrlType.MigrationType))
    service.save(album)

    val expectedMedia = new MediaObjectFixture(storage = UrlType.s3).mediaObject

    service.updateMediaStorageToS3(album.id, expectedMedia.url)

    service.findOne(album.id).map(_.frontPhotoUrl) === Some(expectedMedia)
  }

  @Test
  def updateMediaStorageToCDN {
    val album = new PhotoAlbumFixture().photoAlbum
    service.save(album)

    service.updateMediaStorageToCDN(album.id)

    service.findOne(album.id).flatMap(_.frontPhotoUrl.mediaType) === None
  }

  @Test
  def getUserDefaultPhotoAlbumId {
    val album = new PhotoAlbumFixture().photoAlbum.copy(default = Some(true))
    service.save(album)

    service.getUserDefaultPhotoAlbumId(album.userId) === Some(album.id)
  }

  @Test
  def getUserDefaultPhotoAlbumIdEmtpyWhenHasnt {
    val album = new PhotoAlbumFixture().photoAlbum
    service.save(album)

    service.getUserDefaultPhotoAlbumId(album.userId) === None
  }

  @Test
  def findPhotoAlbumsByIds {
    val album = new PhotoAlbumFixture().photoAlbum
    service.save(album)

    service.findPhotoAlbumsByIds(Set(album.id)) === Seq(album)
  }

}

class PhotoAlbumFixture {
  val photoAlbum = PhotoAlbum(
      name = "name",
      description = Some("description"),
      frontPhotoUrl = MediaObject(MediaObjectId(HashHelper.str("photoUrl"))),
      allowedView = Seq(),
      allowedComment = Seq(),
      userId = UserId(),
      count = 0
  )
}