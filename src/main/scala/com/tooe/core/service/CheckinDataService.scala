package com.tooe.core.service

import com.tooe.core.db.mongo.domain.{CheckinBaseProjection, Checkin}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.CheckinRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.domain._
import org.springframework.data.mongodb.core.geo.{Point, Circle, Distance, Metrics}
import scala.collection.JavaConverters._
import org.springframework.data.domain.Sort
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.CheckinId
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.Urls

trait CheckinDataService {
  def save(checkin: Checkin): Checkin

  def findOne(id: CheckinId): Option[Checkin]

  def removeUserCheckins(id: UserId): Unit

  def findUsersCheckin(id: UserId) : Option[Checkin]

  def findCheckedInUsers(userIds: Seq[UserId], locationId: LocationId) : Seq[UserId]

  def findCheckedInUsers(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit) : Seq[UserId]

  def countCheckedInUsers(locationId: LocationId, currentUserId: UserId) : Long

  def countCheckedInFriends(locationId: LocationId, currentUserId: UserId) : Long

  def findCheckedInFriends(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit) : Seq[UserId]

  def findCheckins(locationIds: Seq[LocationId]) : Seq[Checkin]

  def searchNearOrderedByDistance(request: SearchNearParams, radiusM: Int) : Seq[CheckinWithDistance]

  def searchNearCount(request: SearchNearParams, radiusM: Int): Long

  def searchNearOrderedByName(request: SearchNearParams, radiusM: Int) : Seq[CheckinWithDistance]

  def changeUrlTypeToCdn(url: Urls): Unit
}

@Service
class CheckinDataServiceImpl extends CheckinDataService {
  @Autowired var repo: CheckinRepository = _
  @Autowired var mongo: MongoTemplate = _

  import com.tooe.core.db.mongo.query._

  val entityClass = classOf[Checkin]

  def save(checkin: Checkin): Checkin = repo.save(checkin)

  def findOne(id: CheckinId) = Option(repo.findOne(id.id))

  def findCheckedInUsers(userIds: Seq[UserId], locationId: LocationId): Seq[UserId] = mongo.find(
      Query.query(new Criteria("u.uid").in(userIds.map(_.id).asJavaCollection)
        .and("lo.lid").is(locationId.id)
      ),
      entityClass
  ).asScala.toSeq.map(c => c.user.userId)

  def findUsersCheckin(id: UserId): Option[Checkin] = Option(repo.searchUserCheckin(id.id))

  def removeUserCheckins(id: UserId) = mongo.remove(
    Query.query(new Criteria("u.uid").is(id.id)), entityClass
  )

  def findCheckedInUsers(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit): Seq[UserId] = mongo.find(
   locationCheckinsQuery(locationId, currentUserId: UserId, offsetLimit: OffsetLimit),
    entityClass
  ).asScala.toSeq.map(c => c.user.userId)

  def countCheckedInUsers(locationId: LocationId, currentUserId: UserId): Long = {
    mongo.count(locationCheckinsQuery(locationId, currentUserId: UserId, OffsetLimit()), entityClass)
  }

  def countCheckedInFriends(locationId: LocationId, currentUserId: UserId): Long =
    mongo.count(locationFriendCheckinsQuery(locationId, currentUserId, OffsetLimit()), entityClass)

  def findCheckedInFriends(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit): Seq[UserId] = mongo.find(
    locationFriendCheckinsQuery(locationId, currentUserId, offsetLimit),
    entityClass
  ).asScala.toSeq.map(c => c.user.userId)   //todo  projection instead of checkin

  private def locationCheckinsQuery(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit) =
    Query.query(locationCheckinsCriteria(locationId, currentUserId)).withPaging(offsetLimit)

  private def locationFriendCheckinsQuery(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit) =
    Query.query(locationCheckinsCriteria(locationId, currentUserId).and("fs").all(currentUserId.id)).withPaging(offsetLimit)

  private def locationCheckinsCriteria(locationId: LocationId, currentUserId: UserId) =
    new Criteria("lo.lid").is(locationId.id).and("u.uid").ne(currentUserId.id)


  def findCheckins(locationIds: Seq[LocationId]): Seq[Checkin] = mongo.find(
    Query.query(new Criteria("lo.lid").in(locationIds.map(_.id).asJavaCollection)),
    entityClass
  ).asScala.toSeq

  def searchNearOrderedByDistance(request: SearchNearParams, radiusM: Int) = {
    //TODO could use mongoTemplate with Distance.getNormalizedValue
    val result = repo.nearSphere(
      lon = request.coords.longitude,
      lat = request.coords.latitude,
      maxDistance = radiusM,
      excludeUserId = request.userId.id,
      SkipLimitSort(request.offsetLimit)
    ).asScala

    result map { checkin =>
      CheckinWithDistance(checkin, Some(checkin.coords distanceKm request.coords))
    }
  }
  
  def searchNearCount(request: SearchNearParams, radiusM: Int) =
    repo.nearSphereCount(
      lon = request.coords.longitude,
      lat = request.coords.latitude,
      maxDistance = radiusM,
      excludeUserId = request.userId.id
    )

  def searchNearOrderedByName(request: SearchNearParams, radiusM: Int): Seq[CheckinWithDistance] = {
    def checkinsBaseCriteria = Criteria.where("lo.l").withinSphere(searchCircle(request, radiusM))
      .and("u.uid").ne(request.userId.id)

    val query = Query.query(checkinsBaseCriteria).withPaging(request.offsetLimit)
      .sort(Sort.Direction.ASC, "u.n", "u.ln" )

    query.fields().exclude("fs")

    val checkins = mongo.find(query, entityClass).asScala.toSeq

    checkins.map(resultItem => CheckinWithDistance(resultItem, None))
  }

  private def searchCircle(request: SearchNearParams, radiusM: Int) = {
    val center = new Point(request.coords.longitude, request.coords.latitude)
    val distance = new Distance(radiusM / 1000, Metrics.KILOMETERS).getNormalizedValue
    new Circle(center, distance)
  }

  def changeUrlTypeToCdn(url: Urls) {
    val field = url.entityType match {
      case EntityType.checkinLocation => "lo"
      case EntityType.checkinUser => "u"
    }

    val query = Query.query(Criteria.where("_id").is(url.entityId))
    val update = new Update().unset(s"${field}.m.t")

    mongo.updateFirst(query, update, entityClass)
  }
}

case class CheckinWithDistance(checkin: CheckinBaseProjection, distance: Option[Double])

case class SearchNearParams
(
  coords: Coordinates,
  offsetLimit: OffsetLimit,
  userId: UserId
  )