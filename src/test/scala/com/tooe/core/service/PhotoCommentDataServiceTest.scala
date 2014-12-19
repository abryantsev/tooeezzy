package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.domain.PhotoComment
import com.tooe.core.domain.{UserId, PhotoId}
import org.bson.types.ObjectId
import java.util.Date
import org.junit.Test
import com.tooe.api.service.OffsetLimit

class PhotoCommentDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: PhotoCommentDataServiceImpl = _

  def getEntity(photoId: PhotoId = PhotoId(new ObjectId), userId: UserId = UserId(new ObjectId), time: Date = new Date, message: String = "Test message" ) =
    PhotoComment(
      photoObjectId = photoId.id,
      time = time,
      authorObjId = userId.id,
      message = message
    )

  def generateEntities(count: Int, photoId: PhotoId = PhotoId(new ObjectId)) = {
    val now = new Date
    (1 to count).foldLeft(List.empty[PhotoComment])((list, value) => list :+ getEntity(time = new Date(now.getTime - value), photoId = photoId))
  }

  def saveEntities(entities: List[PhotoComment]) {
    entities.foreach(service.save)
  }

  @Test
  def saveAndRead {
    val entity = getEntity()
    service.findOne(entity.photoCommentId) === None
    service.save(entity) === entity
    service.findOne(entity.photoCommentId) === Some(entity)
  }

  @Test
  def delete {
    val entity = getEntity()
    service.save(entity)
    service.findOne(entity.photoCommentId) === Some(entity)
    service.delete(entity.photoCommentId)
    service.findOne(entity.photoCommentId) === None
  }

  @Test
  def findLastPhotoComments {

    val photoId = PhotoId(new ObjectId)

    val entities = generateEntities(10, photoId)
    saveEntities(entities)

    val result = service.findPhotoComments(photoId, OffsetLimit(None, Some(5)))

    result must haveSize(5)
    result must haveTheSameElementsAs(entities.take(5))

  }

  @Test
  def commentsCount {

    val photoId = PhotoId(new ObjectId)

    val entities = generateEntities(5, photoId)
    saveEntities(entities)

    val result = service.commentsCount(photoId)
    result === 5

  }

}
