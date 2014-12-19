package com.tooe.core.service

import com.tooe.core.db.mongo.domain.LocationCategory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.LocationCategoryRepository
import com.tooe.core.domain.{CategoryField, LocationCategoryId}
import scala.collection.JavaConverters._
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.util.ProjectionHelper
import ProjectionHelper._
import org.springframework.data.mongodb.core.query.Query

trait LocationCategoryDataService  {
  def findOne(id: LocationCategoryId): Option[LocationCategory]
  def save(entity: LocationCategory): LocationCategory
  def getLocationsCategories(locationIds: Seq[LocationCategoryId]): Seq[LocationCategory]
  def findCategoriesBy(fields: Seq[CategoryField]): Seq[LocationCategory]
}

@Service
class LocationCategoryDataServiceImpl extends LocationCategoryDataService {
  @Autowired var repo: LocationCategoryRepository = _
  @Autowired var mongo: MongoTemplate = _

  val fieldsProjection: Map[CategoryField, String] = {
    import CategoryField._
    Map(
      Id    -> "_id",
      Name  -> "n",
      Media -> "cm"
    )
  }

  def findOne(id: LocationCategoryId) = Option(repo.findOne(id.id))

  def save(entity: LocationCategory) = repo.save(entity)

  def getLocationsCategories(locationIds: Seq[LocationCategoryId]): Seq[LocationCategory] = repo.getLocationsCategories(locationIds.map(_.id)).asScala.toSeq

  def findCategoriesBy(fields: Seq[CategoryField]) =
    mongo.find(new Query().extendProjection(fields.map(fieldsProjection)), classOf[LocationCategory]).asScala.toSeq

}