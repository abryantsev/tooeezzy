package com.tooe.core.service

import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.LocationPhoto
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LocationPhotoRepository
import scala.collection.JavaConverters._
import com.tooe.core.domain._
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.query.SkipLimitSort
import com.tooe.api.service.{ChangeLocationPhotoRequest, OffsetLimit}
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.converters.{DBObjectConverters, DBCommonConverters}
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.LocationPhoto
import com.tooe.core.domain.LocationId
import com.tooe.api.service.ChangeLocationPhotoRequest
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.domain.LocationPhotoId

trait LocationPhotoDataService {

  def findOne(id: LocationPhotoId): Option[LocationPhoto]

  def save(photo: LocationPhoto): LocationPhoto

  def getLocationPhotos(photoIds: Seq[LocationPhotoId]): Seq[LocationPhoto]

  def updateUserLikes(locationPhotoId: LocationPhotoId, userId: UserId): Unit

  def updateUserLikes(locationPhotoId: LocationPhotoId, userIds: Seq[UserId]): Unit

  def addUserComment(locationPhotoId: LocationPhotoId, userId: UserId): Unit

  def delete(id: LocationPhotoId): Unit

  def getLastLocationPhotos(locationId: LocationId): List[LocationPhoto]

  def getAllLocationPhotosByAlbum(albumId: LocationPhotoAlbumId): List[LocationPhoto]

  def deletePhotosByAlbum(albumId: LocationPhotoAlbumId): Unit

  def countByLocation(locationId: LocationId): Long

  def getLocationPhotos(albumId: LocationPhotoAlbumId, offsetLimit: OffsetLimit): List[LocationPhoto]

  def changePhoto(locationPhotoId: LocationPhotoId, request: ChangeLocationPhotoRequest): Unit

  def updateMediaStorageToS3(locationPhotoId: LocationPhotoId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(locationPhotoId: LocationPhotoId): Unit

}

@Service
class LocationPhotoDataServiceImpl extends LocationPhotoDataService {
  @Autowired var repo: LocationPhotoRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBCommonConverters._
  import DBObjectConverters._

  val likesCappedArraySize = -20
  val likesCappedCommentSize = -20

  val entityClass = classOf[LocationPhoto]

  def findOne(id: LocationPhotoId) = Option(repo.findOne(id.id))

  def save(photo: LocationPhoto) = repo.save(photo)

  def getLocationPhotos(photoIds: Seq[LocationPhotoId]): Seq[LocationPhoto] =
    repo.getLocationPhotos(photoIds.map(_.id)).asScala.toSeq

  def updateUserLikes(locationPhotoId: LocationPhotoId, userId: UserId) = {
    val query = Query.query(new Criteria("id").is(locationPhotoId.id))
    val update = (new Update).inc("lc", 1).push("ls", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", likesCappedArraySize) )
    mongo.updateFirst(query, update, entityClass)
  }

  def updateUserLikes(locationPhotoId: LocationPhotoId, userIds: Seq[UserId]) = {
    val query = Query.query(new Criteria("id").is(locationPhotoId.id))
    val update = (new Update).inc("lc", -1).set("ls", userIds.map(_.id))
    mongo.updateFirst(query, update, entityClass)
  }

  def addUserComment(locationPhotoId: LocationPhotoId, userId: UserId) = {
    val query = Query.query(new Criteria("id").is(locationPhotoId.id))
    val update = (new Update).inc("cc", 1).push("cu", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", likesCappedCommentSize) )
    mongo.updateFirst(query, update, entityClass)
  }

  def delete(id: LocationPhotoId) { repo.delete(id.id) }

  def getLastLocationPhotos(locationId: LocationId) = repo.findByLocationId(locationId.id, SkipLimitSort(0, 20).desc("t")).asScala.toList

  def getAllLocationPhotosByAlbum(albumId: LocationPhotoAlbumId) = repo.findByAlbumId(albumId.id).asScala.toList

  def deletePhotosByAlbum(albumId: LocationPhotoAlbumId) {
    val removeQuery = Query.query(Criteria.where("pid").is(albumId.id))
    mongo.remove(removeQuery, entityClass)
  }

  def countByLocation(locationId: LocationId) = {
    val query = Query.query(Criteria.where("lid").is(locationId.id))
    mongo.count(query, entityClass)
  }

  def getLocationPhotos(albumId: LocationPhotoAlbumId, offsetLimit: OffsetLimit) =
    repo.findByAlbumId(albumId.id, SkipLimitSort(offsetLimit).desc("t")).asScala.toList

  def changePhoto(locationPhotoId: LocationPhotoId, request: ChangeLocationPhotoRequest) {
    val query = Query.query(new Criteria("_id").is(locationPhotoId.id))
    val update = (new Update).setSkipUnset("n", request.name)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToS3(locationPhotoId: LocationPhotoId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("id").is(locationPhotoId.id))
    val update = new Update().set("u.t", UrlType.s3.id).set("u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(locationPhotoId: LocationPhotoId) {
    val query = Query.query(new Criteria("id").is(locationPhotoId.id))
    val update = new Update().unset("u.t")
    mongo.updateFirst(query, update, entityClass)
  }

}