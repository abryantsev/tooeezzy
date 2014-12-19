package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationPhotoLike
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.repository.LocationPhotoLikeRepository
import com.tooe.core.domain.{UserId, LocationPhotoId, LocationPhotoLikeId}
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.db.mongo.query.SkipLimitSort
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import scala.collection.JavaConverters._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.exceptions.ApplicationException

trait LocationPhotoLikeDataService {

  def findOne(id: LocationPhotoLikeId): Option[LocationPhotoLike]

  def save(entity: LocationPhotoLike): LocationPhotoLike

  def delete(photoId: LocationPhotoId, userId: UserId): LocationPhotoLike

  def userLikeExist(photoId: LocationPhotoId, userId: UserId): Boolean

  def getPhotoLikes(photoId: LocationPhotoId, offsetLimit: OffsetLimit): List[LocationPhotoLike]

  def getPhotoLikesCount(photoId: LocationPhotoId): Long

  def getLikesByPhotos(photoIds: Set[LocationPhotoId]): Seq[LocationPhotoLike]

}

@Service
class LocationPhotoLikeDataServiceImpl extends LocationPhotoLikeDataService {
  @Autowired var mongo: MongoTemplate = _
  @Autowired var repo: LocationPhotoLikeRepository = _

  val entityClass = classOf[LocationPhotoLike]

  def findOne(id: LocationPhotoLikeId) = Option(repo.findOne(id.id))

  def save(entity: LocationPhotoLike) = repo.save(entity)

  def delete(photoId: LocationPhotoId, userId: UserId): LocationPhotoLike = {
    val query = Query.query(new Criteria("pid").is(photoId.id).and("uid").is(userId.id))
    Option(mongo.findAndRemove(query, entityClass)).getOrElse(throw ApplicationException(message = "LocationPhotoLike wasn't found to delete"))
  }

  def findByPhotoId(photoId: LocationPhotoId, offset: Int = 0, limit: Int = 20) =
     repo.findByLocationPhotoId(photoId.id,  SkipLimitSort(offset, limit, new Sort(Direction.DESC, "t"))).asScala.toList

  def userLikeExist(photoId: LocationPhotoId, userId: UserId) = {
    val query = Query.query(new Criteria("pid").is(photoId.id).and("uid").is(userId.id))
    mongo.findOne(query, entityClass) != null
  }

  def getPhotoLikes(photoId: LocationPhotoId, offsetLimit: OffsetLimit) = {
    repo.findByLocationPhotoId(photoId.id,  SkipLimitSort(offsetLimit).desc("t")).asScala.toList
  }

  def getPhotoLikesCount(photoId: LocationPhotoId) = {
    val query = Query.query(new Criteria("pid").is(photoId.id))
    mongo.count(query, entityClass)
  }

  def getLikesByPhotos(photoIds: Set[LocationPhotoId]) = {
    val query = Query.query(new Criteria("pid").in(photoIds.map(_.id).asJavaCollection))
    mongo.find(query, entityClass).asScala
  }


}