package com.tooe.core.service

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Update, Criteria, Query}
import com.tooe.core.usecase.statistics.{ArrayOperation, UpdateRegionOrCountryStatistic}
import com.tooe.core.util.BuilderHelper._
import com.tooe.core.db.mongo.domain.StatisticFields
import com.tooe.core.domain.{StarCategoryId, LocationCategoryId}

trait StatisticsService[Model, Id] {
  import StatisticsService._

  def incrementStatistic(id: Id, statistic: UpdateRegionOrCountryStatistic, mongoTemplate: MongoTemplate, entityClass: Class[Model]) {
     val query = Query.query(new Criteria("_id").is(id))
     val update = (new Update).extend(statistic.locations)(locations => _.inc(locationsField, locations))
                              .extend(statistic.promotions)(promotions => _.inc(promotionsField, promotions))
                              .extend(statistic.sales)(sales => _.inc(salesField, sales))
                              .extend(statistic.users)(users => _.inc(usersField, users))
                              .extend(statistic.favorites)(favorites => _.inc(favoritesField, favorites))
                              .extend(statistic.products)(products => _.inc(productsField, products))
                              .extend(statistic.starCategoriesUpdate) { starCategoryUpdate =>
                                val categoryId = starCategoryUpdate.value.id
                                starCategoryUpdate.operation match {
                                  case ArrayOperation.PushToSet => _.addToSet(starCategoriesField, categoryId)
                                  case ArrayOperation.Delete =>
                                    m =>
                                      if (!starExists(starCategoryUpdate.value, id))
                                        m.pull(starCategoriesField, categoryId)
                                      else m
                                }
                              }
                              .extend(statistic.locationCategoriesUpdate){locationCategoryUpdate =>
                                val categoryId = locationCategoryUpdate.value.id
                                locationCategoryUpdate.operation match {
                                  case ArrayOperation.PushToSet => _.addToSet(locationCategoriesField, categoryId)
                                  case ArrayOperation.Delete =>
                                    m =>
                                      if (!locationExists(locationCategoryUpdate.value, id))
                                        m.pull(locationCategoriesField, categoryId)
                                      else m
                                }
                              }
     mongoTemplate.updateFirst(query, update, entityClass)
  }

  def locationExists(id: LocationCategoryId, on: Id): Boolean
  def starExists(id: StarCategoryId, on: Id): Boolean

  def buildQueryForStatistic(searchFields: StatisticFields) = {
    new Criteria().extend(searchFields.favorites)(_.and(favoritesField).gt(0))
                  .extend(searchFields.locations)(_.and(locationsField).gt(0))
                  .extend(searchFields.promotions)(_.and(promotionsField).gt(0))
                  .extend(searchFields.sales)(_.and(salesField).gt(0))
                  .extend(searchFields.users)(_.and(usersField).gt(0))
                  .extend(searchFields.products)(_.and(productsField).gt(0))
                  .extend(searchFields.starCategory)(sc => _.and(starCategoriesField).in(sc.id))
  }
}

object StatisticsService {
  val productsField = "st.pr"
  val favoritesField = "st.f"
  val locationsField = "st.l"
  val promotionsField = "st.p"
  val salesField = "st.s"
  val usersField = "st.u"
  val starCategoriesField = "st.sc"
  val locationCategoriesField = "st.lc"
  val productCategoriesField = "st.prc"
}
