package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.LocationPhotoLike
import com.tooe.core.domain.{UserId, LocationPhotoId}
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.api.service.OffsetLimit

class LocationPhotoLikeDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationPhotoLikeDataService = _

  def getEntity(photoId: LocationPhotoId = LocationPhotoId(new ObjectId), userId: UserId = UserId(new ObjectId), time: Date = new Date ) =
    LocationPhotoLike(
      locationPhotoId = photoId,
      time = time,
      userId = userId
    )

  def generateEntities(count: Int, photoId: LocationPhotoId = LocationPhotoId(new ObjectId), userId: Option[UserId] = None, time: Date = new Date) =
    (1 to count).map { value => getEntity( photoId, userId.getOrElse(UserId(new ObjectId)), time) }

  def saveEntities(likes: Seq[LocationPhotoLike]) = likes.foreach(like => service.save(like))

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def delete {
    val entity = getEntity()
    service.save(entity)
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.locationPhotoId, entity.userId)
    service.findOne(entity.id) === None
  }

  @Test
  def findByPhotoId {
    val photoId = LocationPhotoId()
    val photosLike = generateEntities(count = 3, photoId = photoId)
    val otherLikes = generateEntities(count = 2, photoId = LocationPhotoId())
    saveEntities(photosLike ++ otherLikes)

    val likes = service.getPhotoLikes(photoId, OffsetLimit(0, 10))
    likes must haveSize(3)
    likes must haveTheSameElementsAs(photosLike)

  }

  @Test
  def userLikeExist {
    val photoId = LocationPhotoId()
    val user1 = UserId()
    val user2 = UserId()
    service.save(LocationPhotoLike(userId = user1, locationPhotoId = photoId))

    service.userLikeExist(photoId, user1) === true
    service.userLikeExist(photoId, user2) === false
  }

  @Test
  def getLikesByPhotos {
    val photoId1 = LocationPhotoId()
    val photoId2 = LocationPhotoId()

    val expected = Seq(getEntity(photoId = photoId1), getEntity(photoId = photoId2))
    expected.foreach(service.save)
    service.save(getEntity())

    service.getLikesByPhotos(expected.map(_.locationPhotoId).toSet) === expected
  }

}
