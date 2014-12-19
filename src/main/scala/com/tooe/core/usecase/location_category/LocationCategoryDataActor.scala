package com.tooe.core.usecase.location_category

import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationCategoryDataService
import concurrent.Future
import com.tooe.core.domain.{LocationCategoryId, CategoryField}
import akka.pattern.pipe
import com.tooe.core.usecase.AppActor

object LocationCategoryDataActor{
  final val Id = Actors.LocationCategoryData

  case class GetLocationCategories(categories: Seq[CategoryField])
  case class GetLocationsCategories(locationIds: Seq[LocationCategoryId])
}

class LocationCategoryDataActor extends AppActor{

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationCategoryDataActor._
  lazy val locationCategoryService = BeanLookup[LocationCategoryDataService]

  def receive = {
    case GetLocationCategories(fields) => Future(locationCategoryService.findCategoriesBy(fields)) pipeTo sender
    case GetLocationsCategories(locationIds) => Future(locationCategoryService.getLocationsCategories(locationIds)) pipeTo sender
  }
}
