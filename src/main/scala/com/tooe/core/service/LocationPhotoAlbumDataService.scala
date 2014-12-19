package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LocationPhotoAlbumRepository
import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain._
import com.tooe.api.service.OffsetLimit
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import scala.collection.JavaConverters._
import com.tooe.core.usecase.location_photoalbum.UpdateLocationPhotoAlbumRequest
import com.tooe.core.db.mongo.converters.{MediaObjectConverter, DBObjectConverters}
import com.tooe.core.db.mongo.query._
import com.tooe.core.util.BuilderHelper
import com.tooe.core.domain.MediaObject
import com.tooe.core.domain.LocationId
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.LocationsChainId
import com.tooe.core.usecase.location_photoalbum.UpdateLocationPhotoAlbumRequest
import com.tooe.core.domain.LocationPhotoAlbumId

trait LocationPhotoAlbumDataService {
  def countByLocation(locationId: LocationId): Long
  def albumsByLocation(locationId: LocationId, offsetLimit: OffsetLimit): List[LocationPhotoAlbum]
  def updatePhotosCount(albumId: LocationPhotoAlbumId, delta: Int): Unit
  def delete(photoAlbumId: LocationPhotoAlbumId): Unit
  def update(albumId: LocationPhotoAlbumId, request: UpdateLocationPhotoAlbumRequest): Unit
  def save(entity: LocationPhotoAlbum): LocationPhotoAlbum
  def findOne(photoAlbumId: LocationPhotoAlbumId): Option[LocationPhotoAlbum]
  def countChainsAlbums(locationsChainId: LocationsChainId): Long
  def findChainsAlbums(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit): Seq[LocationPhotoAlbum]
  def updateMediaStorageToS3(photoAlbumId: LocationPhotoAlbumId, newMedia: MediaObjectId): Unit
  def updateMediaStorageToCDN(photoAlbumId: LocationPhotoAlbumId): Unit
}

@Service
class LocationPhotoAlbumDataServiceImpl extends LocationPhotoAlbumDataService with MediaObjectConverter {
  @Autowired var repo: LocationPhotoAlbumRepository = _
  @Autowired var mongo: MongoTemplate = _

  import DBObjectConverters._
  import BuilderHelper._

  val entityClass = classOf[LocationPhotoAlbum]

  def countByLocation(locationId: LocationId) = mongo.count(Query.query(Criteria.where("lid").is(locationId.id)), classOf[LocationPhotoAlbum])

  def albumsByLocation(locationId: LocationId, offsetLimit: OffsetLimit) =
    repo.findByLocationId(locationId.id, SkipLimitSort(offsetLimit)).asScala.toList

  def updatePhotosCount(albumId: LocationPhotoAlbumId, delta: Int) = {
    val update = (new Update).inc("c", delta)
    val query = Query.query(new Criteria("_id").is(albumId.id))
    mongo.updateFirst(query, update, entityClass)
  }

  def delete(photoAlbumId: LocationPhotoAlbumId) { repo.delete(photoAlbumId.id) }

  def update(albumId: LocationPhotoAlbumId, request: UpdateLocationPhotoAlbumRequest) {
    val query = Query.query(new Criteria("id").is(albumId.id))
    val update = (new Update).setOrSkip("n", request.name)
                             .setSkipUnset("d",request.description)
                             .extend(request.photoUrl)(url => _.setSerialize("p", url))
    mongo.updateFirst(query, update, entityClass)
  }

  def save(entity: LocationPhotoAlbum) = repo.save(entity)

  def findOne(photoAlbumId: LocationPhotoAlbumId) = Option(repo.findOne(photoAlbumId.id))

  private def locationsChainQuery(locationsChainId: LocationsChainId) = Query.query(new Criteria("lcid").is(locationsChainId.id))

  def countChainsAlbums(locationsChainId: LocationsChainId) = mongo.count(locationsChainQuery(locationsChainId), entityClass)

  def findChainsAlbums(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit) =
    mongo.find(locationsChainQuery(locationsChainId).withPaging(offsetLimit), entityClass).asScala

  def updateMediaStorageToS3(photoAlbumId: LocationPhotoAlbumId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("id").is(photoAlbumId.id))
    val update = new Update().set("p.t", UrlType.s3.id).set("p.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(photoAlbumId: LocationPhotoAlbumId) {
    val query = Query.query(new Criteria("id").is(photoAlbumId.id))
    val update = new Update().unset("p.t")
    mongo.updateFirst(query, update, entityClass)
  }

}