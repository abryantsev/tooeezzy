package com.tooe.core.service

import com.tooe.core.domain.{StarCategoryField, StarCategoryId}
import com.tooe.core.db.mongo.domain.StarCategory
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.StarsCategoriesRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.util.ProjectionHelper
import scala.collection.JavaConverters._
import ProjectionHelper._

trait StarsCategoriesDataService {
  def findOne(id: StarCategoryId): Option[StarCategory]
  def save(entity: StarCategory): StarCategory
  def find(fields: Set[StarCategoryField]): Seq[StarCategory]
  def findByIds(ids: Seq[StarCategoryId]): Seq[StarCategory]
  def updateSubscribers(categoryId: StarCategoryId, delta: Int): Unit
}

@Service
class StarsCategoriesDataServiceImpl extends StarsCategoriesDataService {
  @Autowired var repo: StarsCategoriesRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[StarCategory]

  val fieldsProjection: Map[StarCategoryField, String] = {
    import StarCategoryField._
    Map(
      Name -> "n",
      Id -> "_id",
      Description -> "d",
      Media -> "cm",
      StarsCounter -> "c"
    )
  }

  def findOne(id: StarCategoryId) = Option(repo.findOne(id.id))

  def save(entity: StarCategory) = repo.save(entity)

  def find(fields: Set[StarCategoryField]) = {
    val query = new Query().extendProjection(fields map fieldsProjection)
    mongo.find(query, entityClass).asScala.toSeq
  }

  def updateSubscribers(categoryId: StarCategoryId, delta: Int) {
    val query = Query.query(new Criteria("_id").is(categoryId.id))
    val update = (new Update).inc("c", delta)
    mongo.updateFirst(query, update, entityClass)
  }

  def findByIds(ids: Seq[StarCategoryId]) = {
    val query = Query.query(new Criteria("_id").in(ids.map(_.id).asJavaCollection))
    mongo.find(query, entityClass).asScala.toSeq
  }
}