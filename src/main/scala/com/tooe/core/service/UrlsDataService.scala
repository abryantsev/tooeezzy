package com.tooe.core.service

import com.tooe.core.domain.{MediaObjectId, UrlType, UrlsId}
import com.tooe.core.db.mongo.domain.Urls
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.UrlsRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import org.springframework.data.domain.Sort
import scala.collection.JavaConverters._
import java.util.Date
import org.bson.types.ObjectId

trait UrlsDataService {

  def save(entity: Urls): Urls

  def findOne(id: UrlsId): Option[Urls]

  def delete(id: UrlsId): Unit

  def delete(criteria: Seq[(ObjectId, MediaObjectId)]): Unit

  def getLastUrls(size: Int, urlType: UrlType): Seq[Urls]

  def setReadTime(ids: Seq[UrlsId], date: Date): Unit

}

@Service
class UrlsDataServiceImpl extends UrlsDataService {
  @Autowired var repo: UrlsRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Urls]

  def save(entity: Urls) = repo.save(entity)

  def findOne(id: UrlsId) = Option(repo.findOne(id.id))

  def delete(id: UrlsId) { repo.delete(id.id) }

  def getLastUrls(size: Int, urlType: UrlType) = {
    val now = new Date
    val query = Query.query(
      new Criteria().orOperator(
        new Criteria("rt").exists(false).and("t").lte(now).and("ut").is(urlType.id),
        new Criteria("rt").lte(now).and("ut").is(urlType.id)))
      .limit(size).`with`(new Sort(Sort.Direction.ASC, "t"))
    mongo.find(query, entityClass).asScala
  }

  def setReadTime(ids: Seq[UrlsId], date: Date) {
    val query = Query.query(new Criteria("_id").in(ids.map(_.id).asJavaCollection))
    val update = new Update().set("rt", date)
    mongo.updateMulti(query, update, entityClass)
  }

  def delete(criteria: Seq[(ObjectId, MediaObjectId)]) = {
    val (ids, urls) = criteria.unzip
    val query = Query.query(new Criteria("eid").in(ids.asJavaCollection).and("uri").in(urls.map(_.id).asJavaCollection))
    mongo.remove(query, entityClass)
  }
}