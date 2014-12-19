package com.tooe.core.service

import com.tooe.core.db.mongo.domain.PhotoComment
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PhotoCommentRepository
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.query.SkipLimitSort
import scala.collection.JavaConverters._
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import com.tooe.core.domain.{PhotoId, PhotoCommentId}
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.exceptions.ApplicationException

trait PhotoCommentDataService {

  def findOne(id: PhotoCommentId): Option[PhotoComment]

  def save(entity: PhotoComment): PhotoComment

  def findPhotoComments(photoId: PhotoId, offsetLimit: OffsetLimit): Seq[PhotoComment]

  def delete(photoCommentId: PhotoCommentId): PhotoComment

  def commentsCount(photoId: PhotoId): Long

}

@Service
class PhotoCommentDataServiceImpl extends PhotoCommentDataService {
  @Autowired var repo: PhotoCommentRepository = _
  @Autowired var mongo: MongoTemplate = _

  def findOne(id: PhotoCommentId) = Option(repo.findOne(id.id))

  def save(entity: PhotoComment) = repo.save(entity)

  def findPhotoComments(photoId: PhotoId, offsetLimit: OffsetLimit): Seq[PhotoComment] =
    repo.findByPhotoObjectId(photoId.id, SkipLimitSort(offsetLimit).desc("t")).asScala.toSeq

  def delete(photoCommentId: PhotoCommentId): PhotoComment = {
    val query = Query.query(new Criteria("id").is(photoCommentId.id))
    Option(mongo.findAndRemove(query, classOf[PhotoComment])).getOrElse(throw ApplicationException(message = "PhotoComment wasn't found to delete"))
  }

  def commentsCount(photoId: PhotoId) = {
    val query = Query.query(new Criteria("pid").is(photoId.id))
    mongo.count(query, classOf[PhotoComment])
  }

}