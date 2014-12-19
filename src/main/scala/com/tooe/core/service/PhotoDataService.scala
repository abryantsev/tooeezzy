package com.tooe.core.service

import com.tooe.core.db.mongo.domain.Photo
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PhotoRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import scala.collection.JavaConverters._
import com.tooe.core.domain._
import com.mongodb.BasicDBObject
import com.tooe.api.service.{PhotoChangeRequest, OffsetLimit}
import com.tooe.core.db.mongo.query._
import com.tooe.core.db.mongo.converters.DBObjectConverters
import com.tooe.core.domain.UserId
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.api.service.PhotoChangeRequest
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.domain.PhotoId
import org.springframework.data.domain.Sort

trait PhotoDataService {

  def save(entity: Photo): Photo

  def findOne(id: PhotoId): Option[Photo]

  def removePhotos(albumId: PhotoAlbumId): Unit

  def findByAlbumId(albumId: PhotoAlbumId): List[Photo]

  def findByAlbumId(albumId: PhotoAlbumId, offsetLimit: OffsetLimit): List[Photo]

  def getPhotos(photoIds: Seq[PhotoId]): List[Photo]

  def countPhotosInAlbum(albumId: PhotoAlbumId): Long

  def delete(id: PhotoId): Unit

  def updateUserLikes(id: PhotoId, userIds: UserId): Unit

  def updateUserDislikes(photoId: PhotoId, userIds: List[UserId]): Unit

  def addUserComment(id: PhotoId, userIds: UserId): Unit

  def deleteUserComment(id: PhotoId, userIds: Seq[UserId]): Unit

  def getLastUserPhotos(userId: UserId): List[Photo]

  def updatePhoto(id: PhotoId, request: PhotoChangeRequest): Unit

  def updateMediaStorageToS3(id: PhotoId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(id: PhotoId): Unit

}

@Service
class PhotoDataServiceImpl extends PhotoDataService {
  @Autowired var repo: PhotoRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBObjectConverters._

  val entityClass = classOf[Photo]

  def save(entity: Photo) = repo.save(entity)

  def findOne(id: PhotoId) = Option(repo.findOne(id.id))

  def removePhotos(albumId: PhotoAlbumId) {
    val removeQuery = Query.query(Criteria.where("pa").is(albumId.id))
    mongo.remove(removeQuery, classOf[Photo])
  }

  def findByAlbumId(albumId: PhotoAlbumId) =
    repo.findByPhotoAlbumObjectId(albumId.id).asScala.toList

  def findByAlbumId(albumId: PhotoAlbumId, offsetLimit: OffsetLimit) = {
    val query = Query.query(Criteria.where("pa").is(albumId.id)).withPaging(offsetLimit).`with`(new Sort(Sort.Direction.DESC, "t"))
    mongo.find(query, classOf[Photo]).asScala.toList
  }


  def getPhotos(photoIds: Seq[PhotoId]): List[Photo] =
    mongo.find(Query.query(Criteria.where("id").in(photoIds.map(_.id).asJavaCollection)), classOf[Photo]).asScala.toList

  def countPhotosInAlbum(albumId: PhotoAlbumId) =
    mongo.count(Query.query(Criteria.where("pa").is(albumId.id)), classOf[Photo])

  def delete(id: PhotoId) = repo.delete(id.id)

  def updateUserLikes(id: PhotoId, userId: UserId) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("lc", 1).push("ls", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", -20) )
    mongo.updateFirst(query, update, classOf[Photo])
  }

  def updateUserDislikes(photoId: PhotoId, userIds: List[UserId]) = {
    val query = Query.query(new Criteria("id").is(photoId.id))
    val update = (new Update).inc("lc", -1).set("ls", userIds.map(_.id))
    mongo.updateFirst(query, update, classOf[Photo])
  }

  def addUserComment(id: PhotoId, userId: UserId) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("cc", 1).push("cu", new BasicDBObject("$each", java.util.Arrays.asList(userId.id)).append("$slice", -20) )
    mongo.updateFirst(query, update, classOf[Photo])
  }

  def deleteUserComment(id: PhotoId, userIds: Seq[UserId]) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).inc("cc", -1).set("cu", userIds.map(_.id))
    mongo.updateFirst(query, update, classOf[Photo])
  }

  def getLastUserPhotos(userId: UserId) = {
    val query = Query.query(Criteria.where("uid").is(userId.id)).withPaging(OffsetLimit(0, 6)).`with`(new Sort(Sort.Direction.DESC, "t"))
    mongo.find(query, classOf[Photo]).asScala.toList
  }

  def updatePhoto(id: PhotoId, request: PhotoChangeRequest) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = (new Update).setSkipUnset("n", request.name)
    mongo.updateFirst(query, update, classOf[Photo])
  }

  def updateMediaStorageToS3(id: PhotoId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = new Update().set("u.t", UrlType.s3.id).set("u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(id: PhotoId) {
    val query = Query.query(new Criteria("id").is(id.id))
    val update = new Update().unset("u.t")
    mongo.updateFirst(query, update, entityClass)
  }
}