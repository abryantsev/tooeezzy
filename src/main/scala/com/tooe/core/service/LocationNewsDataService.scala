package com.tooe.core.service

import com.tooe.core.db.mongo.domain.{Photo, LocationNews}
import com.tooe.core.domain._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.repository.LocationNewsRepository
import com.tooe.api.service.{OffsetLimit, ChangeLocationNewsRequest}
import org.springframework.data.mongodb.core.query.{Update, Query, Criteria}
import com.tooe.core.db.mongo.query._
import com.tooe.core.util.Lang
import com.tooe.core.db.mongo.converters.DBObjectConverters
import scala.collection.JavaConverters._
import com.mongodb.BasicDBObject
import com.tooe.core.domain.LocationNewsId
import com.tooe.core.domain.UserId
import com.tooe.api.service.ChangeLocationNewsRequest
import com.tooe.core.domain.LocationId
import com.tooe.core.db.mongo.query.LocalizedField
import com.tooe.core.db.mongo.domain.LocationNews

trait LocationNewsDataService {
  def save(entity: LocationNews): LocationNews
  def findOne(id: LocationNewsId): Option[LocationNews]
  def remove(locationNewsId: LocationNewsId): Unit
  def update(locationNewsId: LocationNewsId, request: ChangeLocationNewsRequest, lang: Lang): Unit
  def getLocationNews(locationId: LocationId, offsetLimit: OffsetLimit): List[LocationNews]
  def getLocationNewsCount(locationId: LocationId): Long
  def getLocationsChainNews(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit): List[LocationNews]
  def getLocationsChainNewsCount(locationsChainId: LocationsChainId): Long
  def updateUserLikes(id: LocationNewsId, userId: UserId): Unit
  def updateUserUnlikes(photoId: LocationNewsId, userIds: List[UserId]): Unit
}

@Service
class LocationNewsDataServiceImpl extends LocationNewsDataService {
  @Autowired var repo: LocationNewsRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBObjectConverters._

  val entityClass = classOf[LocationNews]

  def save(entity: LocationNews) = repo.save(entity)

  def findOne(id: LocationNewsId) = Option(repo.findOne(id.id))

  def remove(locationNewsId: LocationNewsId) { repo.delete(locationNewsId.id) }

  def update(locationNewsId: LocationNewsId, request: ChangeLocationNewsRequest, lang: Lang) {
    val query = Query.query(new Criteria("id").is(locationNewsId.id))
    val update = (new Update).setSkipUnset("cf", request.enableComments)
                            .setOrSkip(LocalizedField("c", lang).value, request.content)
    mongo.updateFirst(query, update, entityClass)
  }

  private def locationQuery(locationId: LocationId) = Query.query(new Criteria("lid").is(locationId.id))

  def getLocationNews(locationId: LocationId, offsetLimit: OffsetLimit) =
    mongo.find(locationQuery(locationId).withPaging(offsetLimit).desc("t"), entityClass).asScala.toList

  def getLocationNewsCount(locationId: LocationId) = mongo.count(locationQuery(locationId), entityClass)

  private def locationsChainQuery(locationsChainId: LocationsChainId) = Query.query(new Criteria("lcid").is(locationsChainId.id))

  def getLocationsChainNews(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit) =
    mongo.find(locationsChainQuery(locationsChainId).withPaging(offsetLimit).desc("t"), entityClass).asScala.toList

  def getLocationsChainNewsCount(locationsChainId: LocationsChainId) = mongo.count(locationsChainQuery(locationsChainId), entityClass)

  def updateUserLikes(id: LocationNewsId, userId: UserId) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("lc", 1).push("ls", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", -10) )
    mongo.updateFirst(query, update, entityClass)
  }

  def updateUserUnlikes(photoId: LocationNewsId, userIds: List[UserId]) {
    val query = Query.query(new Criteria("id").is(photoId.id))
    val update = (new Update).inc("lc", -1).set("ls", userIds.map(_.id).asJava)
    mongo.updateFirst(query, update, entityClass)
  }

}

