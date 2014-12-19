package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationPhotoComment
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.domain.{LocationPhotoId, LocationPhotoCommentId}
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query.SkipLimitSort
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.repository._

trait LocationPhotoCommentDataService {

  def findOne(id: LocationPhotoCommentId): Option[LocationPhotoComment]

  def save(entity: LocationPhotoComment): LocationPhotoComment

  def photoCommentsCount(photoId: LocationPhotoId): Long

  def photoComments(photoId: LocationPhotoId, offsetLimit: OffsetLimit): Seq[LocationPhotoComment]

}

@Service
class LocationPhotoCommentDataServiceImpl extends LocationPhotoCommentDataService {
  @Autowired var mongo: MongoTemplate = _
  @Autowired var repo: LocationPhotoCommentRepository = _

  val entityClass = classOf[LocationPhotoComment]

  def findOne(id: LocationPhotoCommentId) = Option(repo.findOne(id.id))

  def save(entity: LocationPhotoComment) = repo.save(entity)

  def photoCommentsCount(photoId: LocationPhotoId) = {
    val query = Query.query(new Criteria("pid").is(photoId.id))
    mongo.count(query, entityClass)
  }

  def photoComments(photoId: LocationPhotoId, offsetLimit: OffsetLimit) =
    repo.findByLocationPhotoId(photoId.id, SkipLimitSort(offsetLimit).desc("t")).asScala.toSeq

}