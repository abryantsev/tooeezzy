package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{CategoryField, LocationCategoryId}
import com.tooe.core.usecase.AppActor
import akka.actor.{ActorSystem, ActorContext, Actor}
import scala.concurrent.Promise
import com.tooe.core.usecase.location_category.LocationCategoryDataActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationCategoryDataService
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.convert.WrapAsScala
import com.tooe.core.exceptions.ApplicationException

@Document(collection = "location_category")
case class LocationCategory
(
  id: LocationCategoryId = LocationCategoryId(),
  name: ObjectMap[String] = ObjectMap.empty,
  categoryMedia: Seq[LocationCategoryMedia] = Nil
  )

case class LocationCategoryMedia(url: String, purpose: Option[String])

object LocationCategory {
  private val p = Promise[Map[LocationCategoryId, LocationCategory]]()

  def categoriesMap(implicit as: ActorContext): Map[LocationCategoryId, LocationCategory] = {
    def fulfill = {
      val locationCategoryService = BeanLookup[LocationCategoryDataService]
      val map = locationCategoryService.findCategoriesBy(fields = CategoryField.values).groupBy(_.id).mapValues(_.head)
      val mapWithDefaults = map.withDefault {
        case lcid if lcid.id.startsWith("location-category") => LocationCategory(id = lcid, name = Map.empty[String, String], Nil)
        case id => throw new ApplicationException(message = "No such location category as" + id.id)
      }
      p.trySuccess(mapWithDefaults)
    }
    fulfill
    if (p.isCompleted) p.future.value.get.get //asian sluts are safer
    else {
      fulfill
      categoriesMap
    }
  }
}