package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import org.bson.types.ObjectId
import com.tooe.core.domain.{UserId, PhotoId}
import com.tooe.core.db.mongo.domain.PhotoLike
import java.util.Date
import com.tooe.core.util.DateHelper

class PhotoLikeDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PhotoLikeDataService = _

  def getEntity(photoId: PhotoId = PhotoId(new ObjectId), userId: UserId = UserId(new ObjectId), time: Date = new Date ) =
    PhotoLike(
      photoId = photoId,
      time = time,
      userId = userId
    )

  def generatePhotoLikes(count: Int, photoId: PhotoId = PhotoId(new ObjectId)) = {
    val now = new Date
    (1 to count).foldLeft(List.empty[PhotoLike])((list, value) => list :+ getEntity(time = new Date(now.getTime - value), photoId = photoId))
  }

  def saveEntities(entities: List[PhotoLike]) {
    entities.foreach(service.save)
  }

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def getLikesByPhoto {

    val photoId = PhotoId(new ObjectId)

    val photoLikes = generatePhotoLikes(20, photoId)
    saveEntities(photoLikes)

    val likes = service.getLikesByPhoto(photoId, 0, 10)
    likes must haveSize(10)
    likes must haveTheSameElementsAs(photoLikes.take(10))

  }

  @Test
  def userLikeExist {
    val photoId = PhotoId()
    val user1 = UserId()
    val user2 = UserId()
    service.save(PhotoLike(userId = user1, photoId = photoId, time = new Date))

    service.userLikeExist(photoId, user1) === true
    service.userLikeExist(photoId, user2) === false
  }

  @Test
  def userPhotosLikes {
    val photoId1 = PhotoId()
    val photoId2 = PhotoId()
    val userId = UserId()

    val like1 = PhotoLike(userId = userId, photoId = photoId1, time = new Date)
    val like2 = PhotoLike(userId = userId, photoId = photoId2, time = new Date)

    service.save(like1)
    service.save(like2)
    service.save(PhotoLike(userId = UserId(), photoId = photoId2, time = new Date))

    val userLikes = service.userPhotosLikes(Seq(photoId1, photoId2, PhotoId()), userId)

    userLikes must haveSize(2)
    userLikes must haveTheSameElementsAs(Seq(like1, like2))

  }

}

class PhotoLikeFixture {
  val photoLike = PhotoLike(
    photoId = PhotoId(),
    time = DateHelper.currentDate,
    userId = UserId()
  )
}
