package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationsChain
import com.tooe.core.domain.{UrlType, MediaObjectId, LocationsChainId}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LocationsChainRepository
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.mongo.query.UpdateResult
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}

trait LocationsChainDataService {

  def save(event: LocationsChain): LocationsChain

  def findOne(id: LocationsChainId): Option[LocationsChain]

  def findAllById(ids: Seq[LocationsChainId]): Seq[LocationsChain]

  def changeLocationCounter(id: LocationsChainId, delta: Int): UpdateResult

  def updateMediaStorageToS3(id: LocationsChainId, media: MediaObjectId, newMedia: MediaObjectId): Unit

  def updateMediaStorageToCDN(id: LocationsChainId, media: MediaObjectId): Unit

}

@Service
class LocationsChainDataServiceImpl extends LocationsChainDataService {

  import scala.collection.JavaConversions._
  import scala.collection.JavaConverters._

  @Autowired var repo: LocationsChainRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[LocationsChain]

  import com.tooe.core.db.mongo.query._

  def save(event: LocationsChain) = repo.save(event)

  def findOne(id: LocationsChainId) = Option(repo.findOne(id.id))

  def findAllById(ids: Seq[LocationsChainId]) = repo.findAll(ids.map(_.id)).asScala.toSeq

  def changeLocationCounter(id: LocationsChainId, delta: Int) =
    mongo.updateFirst(
      Query.query(new Criteria("_id").is(id.id)),
      new Update().inc("lc", delta),
      entityClass
    ).asUpdateResult

  def updateMediaStorageToS3(id: LocationsChainId, media: MediaObjectId, newMedia: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(id.id).and("cm.u.mu").is(media.id))
    val update = new Update().set("cm.$.u.t", UrlType.s3.id).set("cm.$.u.mu", newMedia.id)
    mongo.updateFirst(query, update, entityClass)
  }

  def updateMediaStorageToCDN(id: LocationsChainId, media: MediaObjectId) {
    val query = Query.query(new Criteria("_id").is(id.id).and("cm.u.mu").is(media.id))
    val update = new Update().unset("cm.$.u.t")
    mongo.updateFirst(query, update, entityClass)
  }
}
