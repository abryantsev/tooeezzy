package com.tooe.core.usecase.location_category

import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import concurrent.Future
import com.tooe.core.domain.{CategoryField, LocationCategoryId}
import akka.pattern.{pipe, ask}
import com.tooe.core.db.mongo.domain.LocationCategory
import com.tooe.core.usecase.AppActor

object LocationCategoryActor{
  final val Id = Actors.LocationCategory

  case class GetLocationCategoryItems(categoryIds: Seq[LocationCategoryId], lang: Lang)
  case class GetProductCategories(fields: Seq[CategoryField], lang: Lang)
}

class LocationCategoryActor extends AppActor {

  lazy val locationCategoryDataActor = lookup(LocationCategoryDataActor.Id)

  import LocationCategoryActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case GetLocationCategoryItems(categoryIds, lang) =>
      getLocationCategories(categoryIds) map (_ map LocationCategoryItem(lang)) pipeTo sender

    case GetProductCategories(fields, lang) =>
      getAllLocationCategories(fields) map (_ map LocationCategoryItem(lang)) pipeTo sender
  }

  def getAllLocationCategories(fields: Seq[CategoryField]) =
    (locationCategoryDataActor ? LocationCategoryDataActor.GetLocationCategories(fields)).mapTo[Seq[LocationCategory]]

  def getLocationCategories(categoryIds: Seq[LocationCategoryId]): Future[Seq[LocationCategory]] =
    (locationCategoryDataActor ? LocationCategoryDataActor.GetLocationsCategories(categoryIds.toSeq)).mapTo[Seq[LocationCategory]]
}

case class LocationCategoryItem
(
  id: LocationCategoryId,
  name: String
  )

object LocationCategoryItem{
  def apply(lang: Lang)(lc: LocationCategory): LocationCategoryItem = LocationCategoryItem(
    lc.id,
    lc.name.localized(lang) getOrElse ""
  )
}
