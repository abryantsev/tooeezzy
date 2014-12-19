package com.tooe.core.service

import com.tooe.core.db.mongo.domain.PhotoAlbum
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.PhotoAlbumRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query, Update}
import com.tooe.core.db.mongo.query._
import scala.collection.JavaConverters._
import com.tooe.core.db.mongo.query.SkipLimitSort
import com.tooe.core.domain._
import com.tooe.core.util.BuilderHelper
import com.tooe.core.usecase.PhotoAlbumWriteActor.EditPhotoAlbumFields
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.converters.MediaObjectConverter
import com.tooe.core.domain.UserId
import com.tooe.core.domain.PhotoAlbumId
import com.tooe.core.usecase.PhotoAlbumWriteActor.EditPhotoAlbumFields
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.db.mongo.domain.PhotoAlbum

trait PhotoAlbumDataService {
  def save(entity: PhotoAlbum): PhotoAlbum

  def findOne(id: PhotoAlbumId): Option[PhotoAlbum]

  def includeUserGroups(id: PhotoAlbumId, userGroups: String*)

  def excludeUserGroups(id: PhotoAlbumId, userGroups: String*)

  def delete(id: PhotoAlbumId)

  def update(id: PhotoAlbumId, editFields: EditPhotoAlbumFields)

  def photoAlbumsCountByUser(userId: UserId): Long

  def findPhotoAlbumsByUser(userId: UserId, offsetLimit: OffsetLimit): List[PhotoAlbum]

  def findPhotoAlbumsByIds(albumId: Set[PhotoAlbumId]): Seq[PhotoAlbum]

  def changePhotoCount(albumId: PhotoAlbumId, count: Int)

  def updateMediaStorageToS3(albumId: PhotoAlbumId, media: MediaObjectId): Unit

  def updateMediaStorageToCDN(albumId: PhotoAlbumId): Unit

  def getUserDefaultPhotoAlbumId(userId: UserId): Option[PhotoAlbumId]
}

@Service
class PhotoAlbumDataServiceImpl extends PhotoAlbumDataService with MediaObjectConverter {
  @Autowired var repo: PhotoAlbumRepository = _
  @Autowired var mongo: MongoTemplate = _

  import BuilderHelper._

  val entityClass = classOf[PhotoAlbum]

  def save(entity: PhotoAlbum) = repo.save(entity)

  def findOne(id: PhotoAlbumId) = Option(repo.findOne(id.id))

  def includeUserGroups(id: PhotoAlbumId, userGroups: String*) {
    mongo.updateFirst(
      Query.query(new Criteria("id").is(id.id)),
      (new Update).pushAll("av", userGroups.toArray),
      entityClass
    )
  }

  def excludeUserGroups(id: PhotoAlbumId, userGroups: String*) {
    mongo.updateFirst(
      Query.query(new Criteria("id").is(id.id)),
      (new Update).pullAll("av", userGroups.toArray),
      classOf[PhotoAlbum]
    )
  }

  def delete(id: PhotoAlbumId) {
    repo.delete(id.id)
  }

  def update(id: PhotoAlbumId, editFields: EditPhotoAlbumFields) {
    val usergroupOpt = editFields.usergroups
    val update = (new Update).setSkipUnset("n", editFields.name)
      .setSkipUnset("d", editFields.description)
      .extend(editFields.photoUrl)(url => _.setSerialize("p", url))
      .extend(usergroupOpt)(usergroup => _.setSkipUnset("ac", usergroup.comments).setSkipUnset("av", usergroup.view))
    val query = Query.query(new Criteria("_id").is(id.id))
    //TODO: if we send empty body "{}" then we unset properties because query will be update({..}, {})
    if (update.getUpdateObject.keySet.size > 0)
      mongo.updateFirst(query, update, entityClass)
  }

  def photoAlbumsCountByUser(userId: UserId) =
    mongo.count(Query.query(Criteria.where("uid").is(userId.id)), entityClass)

  def findPhotoAlbumsByUser(userId: UserId, offsetLimit: OffsetLimit) =
    repo.findByUserObjectId(userId.id, SkipLimitSort(offsetLimit).desc("t")).asScala.toList


  def findPhotoAlbumsByIds(albumIds: Set[PhotoAlbumId]): Seq[PhotoAlbum] =
    mongo.find(Query.query(new Criteria("id").in(albumIds.map(_.id).asJavaCollection)),
      entityClass).asScala.toSeq

  def changePhotoCount(albumId: PhotoAlbumId, count: Int) {
    val update = (new Update).inc("c", count)
    val query = Query.query(new Criteria("_id").is(albumId.id))
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToS3(albumId: PhotoAlbumId, media: MediaObjectId) {
    val query = Query.query(new Criteria("id").is(albumId.id))
    val update = new Update().set("p.t", UrlType.s3.id).set("p.mu", media.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(albumId: PhotoAlbumId) {
    val query = Query.query(new Criteria("id").is(albumId.id))
    val update = new Update().unset("p.t")
    mongo.updateFirst(query, update, entityClass)
  }

  def getUserDefaultPhotoAlbumId(userId: UserId) = {
    val query = Query.query(new Criteria("uid").is(userId.id).and("de").is(true))
    Option(mongo.findOne(query, entityClass)).map(_.id)
  }

}