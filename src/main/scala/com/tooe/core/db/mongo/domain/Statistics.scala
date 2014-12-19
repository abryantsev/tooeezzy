package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.{LocationCategoryId, StarCategoryId}

case class Statistics(locationsCount: Int = 0,
                      promotionsCount: Int = 0,
                      salesCount: Int = 0,
                      usersCount: Int = 0,
                      favoritesCount: Int = 0,
                      presentsCount: Int = 0,
                      starCategories: Seq[StarCategoryId] = Nil,
                      locationCategories: Seq[LocationCategoryId] = Nil)

case class StatisticFields(locations: Boolean = false,
                           promotions: Boolean = false,
                           sales: Boolean = false,
                           users: Boolean = false,
                           favorites: Boolean = false,
                           products: Boolean = false,
                           starCategory: Option[StarCategoryId] = None)