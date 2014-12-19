package com.tooe.core.service

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.repository.PhotoLikeRepository
import com.tooe.core.db.mongo.domain.PhotoLike
import com.tooe.core.domain.{UserId, PhotoId, PhotoLikeId}
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query._
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import com.tooe.core.exceptions.ApplicationException

trait PhotoLikeDataService {

  def findOne(id: PhotoLikeId): Option[PhotoLike]

  def save(entity: PhotoLike): PhotoLike

  def delete(photoId: PhotoId, userId: UserId): PhotoLike

  def getLikesByPhoto(photoId: PhotoId, offset: Int, limit: Int): List[PhotoLike]

  def userLikeExist(photoId: PhotoId, userId: UserId): Boolean

  def userPhotosLikes(photoIds: Seq[PhotoId], userId: UserId): Seq[PhotoLike]
}

@Service
class PhotoLikeDataServiceImpl extends PhotoLikeDataService {
  @Autowired var repo: PhotoLikeRepository = _
  @Autowired var mongo: MongoTemplate = _

  def findOne(id: PhotoLikeId) = Option(repo.findOne(id.id))

  def save(entity: PhotoLike) = repo.save(entity)

  def delete(photoId: PhotoId, userId: UserId): PhotoLike = {
    val query = Query.query(new Criteria("pid").is(photoId.id).and("uid").is(userId.id))
    Option(mongo.findAndRemove(query, classOf[PhotoLike])).getOrElse(throw ApplicationException(message = "PhotoLike wasn't found to delete"))
  }

  def getLikesByPhoto(photoId: PhotoId, offset: Int, limit: Int) =
    repo.findByPhotoId(photoId.id, SkipLimitSort(offset, limit, new Sort(Direction.DESC, "t"))).asScala.toList

  def userLikeExist(photoId: PhotoId, userId: UserId) = {
    val query = Query.query(new Criteria("pid").is(photoId.id).and("uid").is(userId.id))
    mongo.findOne(query, classOf[PhotoLike]) != null
  }

  def userPhotosLikes(photoIds: Seq[PhotoId], userId: UserId) = {
    val query = Query.query(new Criteria("pid").in(photoIds.map(_.id).asJavaCollection).and("uid").is(userId.id))
    mongo.find(query, classOf[PhotoLike]).asScala
  }

}