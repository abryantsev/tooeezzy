package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.LocationPhotoComment
import com.tooe.core.domain.{UserId, LocationPhotoId}
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.api.service.OffsetLimit

class LocationPhotoCommentDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationPhotoCommentDataService = _

  def getEntity(photoId: LocationPhotoId = LocationPhotoId(new ObjectId), userId: UserId = UserId(new ObjectId), time: Date = new Date, message: String = "Comment" ) =
    LocationPhotoComment(
      locationPhotoId = photoId,
      time = time,
      userId = userId,
      message = message
    )

  def saveEntities(comments: Seq[LocationPhotoComment]) = comments.foreach(service.save)

  def generate(count: Int, photoId: LocationPhotoId = LocationPhotoId(new ObjectId), userId: UserId = UserId(new ObjectId), time: Date = new Date, message: String = "Comment") =
    (1 to count).map { value =>
      getEntity(photoId, userId, time, message)
    }

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
  }

  @Test
  def countByPhoto {
    val locationPhotoId = LocationPhotoId(new ObjectId)
    val locationPhotos = generate(5, photoId = locationPhotoId)
    val otherLocationPhotos = generate(3)
    saveEntities(locationPhotos ++ otherLocationPhotos)

    service.photoCommentsCount(locationPhotoId) === 5
  }

  @Test
  def photoComments {
    val locationPhotoId = LocationPhotoId(new ObjectId)
    val locationPhotos = generate(5, photoId = locationPhotoId)
    val otherLocationPhotos = generate(3)
    saveEntities(locationPhotos ++ otherLocationPhotos)

    val photoComments = service.photoComments(locationPhotoId, OffsetLimit(None, None))
    photoComments must haveSize(5)
    photoComments must haveTheSameElementsAs(locationPhotos)
  }

}
