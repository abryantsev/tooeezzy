package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.{StatisticFields, Statistics, Region}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.domain.{LocationCategoryId, Coordinates, RegionId, StarCategoryId}
import com.tooe.core.usecase.statistics.{ArrayOperation, StarCategoriesUpdate, UpdateRegionOrCountryStatistic}

class RegionDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: RegionDataService = _

  lazy val entities = new MongoDaoHelper("region")

  @Test
  def saveAndRead {
    val f = new RegionFixture
    import f._
    service.findOne(region.id.id) === None
    service.save(region) === region
    service.findOne(region.id.id) === Some(region)
  }

  @Test
  def representation {
    val f = new RegionFixture
    import f._
    service.save(region)
    val repr = entities.findOne(region.id.id)
    jsonAssert(repr)(s"""{
      "_id" : { "$$oid" : "${region.id.id.toString}" } ,
      "cid" : "${region.countryId.id}" ,
      "n" : { "ru" : "name" },
      "ic" : true,
      "c" : { "lon": 0.0 , "lat": 0.0},
      "st" : {
         "l" : 0,
         "p" : 0,
         "u" : 0,
         "f" : 0,
         "pr" : 0,
         "s" : 0,
         "sc" : [],
         "lc" : []
      }
    }""")
  }

  @Test
  def findByCountryId {
    val f = new RegionFixture
    import f._
    service.findByCountryId(region.countryId) === Nil
    service.save(region)
    service.findByCountryId(region.countryId) === Seq(region)
  }

  @Test
  def updateStatistic {
    val f = new RegionFixture
    import f._
    service.save(region)

    val starCategoryId = StarCategoryId("star category")

    val update = UpdateRegionOrCountryStatistic(Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(StarCategoriesUpdate(starCategoryId, ArrayOperation.PushToSet)))
    service.updateStatistic(region.id, update)
    val updatedEntity = service.find(region.id)

    val statistic = updatedEntity.statistics
    statistic.favoritesCount === update.favorites.get
    statistic.locationsCount === update.locations.get
    statistic.promotionsCount === update.promotions.get
    statistic.salesCount === update.sales.get
    statistic.usersCount === update.users.get
    statistic.starCategories === update.starCategoriesUpdate.map(_.value).toSeq

    val deleteStarCategory = UpdateRegionOrCountryStatistic(starCategoriesUpdate = Some(StarCategoriesUpdate(starCategoryId, ArrayOperation.Delete)))
    service.updateStatistic(RegionId(region.id.id), deleteStarCategory)
    val countryWithoutStarCategory = service.find(RegionId(region.id.id))
    countryWithoutStarCategory.statistics.starCategories === Nil
  }

  @Test
  def findByStatistics {
    val f = new RegionFixture
    import f._
    val entity = region.copy(statistics = Statistics(1, 1, 1, 1, 1, 1, Seq(StarCategoryId("star_category")), Seq(LocationCategoryId("loc"))))
    service.save(entity)
    val countryId = region.countryId

    val countriesWithUser = service.findByStatistics(countryId, StatisticFields(users = true))
    countriesWithUser.contains(entity) must beTrue

    val countriesWithFavorites = service.findByStatistics(countryId, StatisticFields(favorites = true))
    countriesWithFavorites.contains(entity) must beTrue

    val countriesWithSales = service.findByStatistics(countryId, StatisticFields(sales = true))
    countriesWithSales.contains(entity) must beTrue

    val countriesWithPromotions = service.findByStatistics(countryId, StatisticFields(promotions = true))
    countriesWithPromotions.contains(entity) must beTrue

    val countriesWithLocations = service.findByStatistics(countryId, StatisticFields(locations = true))
    countriesWithLocations.contains(entity) must beTrue

    val countriesWithStarCategory = service.findByStatistics(countryId, StatisticFields(starCategory = entity.statistics.starCategories.headOption))
    countriesWithStarCategory.contains(entity) must beTrue
  }

}


class RegionFixture {
  val region = Region(
    name = Map("ru" -> "name"),
    isCapital = Some(true),
    coordinates = Coordinates(),
    statistics = Statistics()
  )

}