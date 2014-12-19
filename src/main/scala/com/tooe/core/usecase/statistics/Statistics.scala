package com.tooe.core.usecase.statistics

import com.tooe.core.domain.{LocationCategoryId, StarCategoryId}

case class UpdateRegionOrCountryStatistic(locations: Option[Int] = None,
                           promotions: Option[Int] = None,
                           sales: Option[Int] = None,
                           products: Option[Int] = None,
                           users: Option[Int] = None,
                           favorites: Option[Int] = None,
                           starCategoriesUpdate: Option[StarCategoriesUpdate] = None,
                           locationCategoriesUpdate: Option[LocationCategoriesUpdate] = None)

object ArrayOperation extends Enumeration {
  type ArrayOperation = Value

  val PushToSet, Delete = Value

}

case class StarCategoriesUpdate(value: StarCategoryId, operation: ArrayOperation.Value)
case class ProductCategoriesUpdate(value: LocationCategoryId, operation: ArrayOperation.Value)
case class LocationCategoriesUpdate(value: LocationCategoryId, operation: ArrayOperation.Value)