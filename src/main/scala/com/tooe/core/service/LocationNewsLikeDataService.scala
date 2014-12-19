package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationNewsLike
import com.tooe.core.domain.{UserId, LocationNewsLikeId, LocationNewsId}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.repository.LocationNewsLikeRepository
import com.tooe.core.db.mongo.converters.DBObjectConverters
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import scala.collection.JavaConverters._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.exceptions.ApplicationException

trait LocationNewsLikeDataService {
  def save(entity: LocationNewsLike): LocationNewsLike
  def findOne(id: LocationNewsLikeId): Option[LocationNewsLike]
  def remove(LocationNewsLikeId: LocationNewsLikeId): Unit
  def getLikesByUserAndNews(newsIds: Seq[LocationNewsId], userId: UserId): Seq[LocationNewsLike]
  def deleteLike(userId: UserId, locationNewsId: LocationNewsId) : LocationNewsLike
  def getLikes(locationNewsId: LocationNewsId, offsetLimit: OffsetLimit): Seq[LocationNewsLike]
}

@Service
class LocationNewsLikeDataServiceImpl extends LocationNewsLikeDataService {
  @Autowired var repo: LocationNewsLikeRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBObjectConverters._

  val entityClass = classOf[LocationNewsLike]

  def save(entity: LocationNewsLike) = {
    repo.save(entity)
  }

  def findOne(id: LocationNewsLikeId) = Option(repo.findOne(id.id))

  def remove(locationNewsLikeId: LocationNewsLikeId) { repo.delete(locationNewsLikeId.id) }

  def getLikesByUserAndNews(newsIds: Seq[LocationNewsId], userId: UserId) = {
    val query = Query.query(new Criteria("uid").is(userId.id).and("lnid").in(newsIds.map(_.id).asJavaCollection))
    mongo.find(query, entityClass).asScala
  }

  def deleteLike(userId: UserId, locationNewsId: LocationNewsId): LocationNewsLike = {
    val query = Query.query(new Criteria("uid").is(userId.id).and("lnid").is(locationNewsId.id))
    Option(mongo.findAndRemove(query, entityClass)).getOrElse(throw new ApplicationException(message = "LocationNewsLike wasn't found to remove"))
  }

  def getLikes(locationNewsId: LocationNewsId, offsetLimit: OffsetLimit) = {
    import com.tooe.core.db.mongo.query._
    val query = Query.query(new Criteria("lnid").is(locationNewsId.id)).withPaging(offsetLimit)
    mongo.find(query, entityClass).asScala
  }

}

