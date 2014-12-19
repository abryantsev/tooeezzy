package com.tooe.core.service

import org.springframework.beans.factory.annotation.Autowired
import org.junit.Test
import com.tooe.core.db.mongo.domain.{StatisticFields, Statistics, Country}
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import com.tooe.core.util.{Lang, HashHelper}
import com.tooe.core.domain.{LocationCategoryId, CountryField, StarCategoryId, CountryId}
import com.tooe.core.usecase.statistics.{ArrayOperation, StarCategoriesUpdate, UpdateRegionOrCountryStatistic}

class CountryDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: CountryDataService = _

  lazy val entities = new MongoDaoHelper("country")
  implicit val lang = Lang.orig
  val fullSearchFields: Set[CountryField] = {
    import CountryField._
    Set(Id, Name, Phone)
  }

  @Test
  def saveAndRead {
    val entity = new CountryFixture().country
    service.find(entity.id) === None
    service.save(entity) === entity
    service.find(entity.id) === Some(entity)
  }

  @Test
  def findAll {
    val entity = new CountryFixture().country
    val expectedResult = Country(id = entity.id, name = entity.name, phoneCode = entity.phoneCode, pictureFileName = null, statistics = null)

    service.findAll(fullSearchFields) must not contain (expectedResult)
    service.save(entity)

    service.findAll(fullSearchFields).contains(expectedResult) must beTrue
  }

  @Test
  def representation {
    val entity = new CountryFixture().country
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(s"""{
      "_id" : "${entity.id.id}" ,
      "n" : { "en" : "name" } ,
      "pc" : "1" ,
      "ico" : "picture-file-name",
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
  def updateStatistic {
    val entity = new CountryFixture().country
    service.save(entity)

    val starCategoryId = StarCategoryId("star category")

    val update = UpdateRegionOrCountryStatistic(Some(1), Some(1), Some(1), Some(1), Some(1), Some(1), Some(StarCategoriesUpdate(starCategoryId, ArrayOperation.PushToSet)))
    service.updateStatistic(entity.id, update)
    val updatedEntity = service.find(entity.id)

    val statistic = updatedEntity.map(_.statistics)
    statistic.map(_.favoritesCount) === update.favorites
    statistic.map(_.locationsCount) === update.locations
    statistic.map(_.promotionsCount) === update.promotions
    statistic.map(_.salesCount) === update.sales
    statistic.map(_.usersCount) === update.users
    statistic.map(_.starCategories) === Some(update.starCategoriesUpdate.map(_.value).toSeq)

    val deleteStarCategory = UpdateRegionOrCountryStatistic(starCategoriesUpdate = Some(StarCategoriesUpdate(starCategoryId, ArrayOperation.Delete)))
    service.updateStatistic(entity.id, deleteStarCategory)
    val countryWithoutStarCategory = service.find(entity.id)
    countryWithoutStarCategory.map(_.statistics).map(_.starCategories) === Some(Nil)
  }

  @Test
  def findByPhone {
    val country = new CountryFixture().country
    service.save(country)

    val countriesByPhone = service.findByPhone(country.phoneCode, fullSearchFields)

    val expectedResult = Country(id = country.id, name = country.name, phoneCode = country.phoneCode, pictureFileName = null, statistics = null)

    countriesByPhone.contains(expectedResult) must beTrue

  }

  @Test
  def findActive {
    val country = new CountryFixture().country
    service.save(country)

    val activeCountries = service.findActive(fullSearchFields)

    val expectedResult = Country(id = country.id, name = country.name, phoneCode = country.phoneCode, pictureFileName = null, statistics = null)

    activeCountries.contains(expectedResult) must beTrue

  }

  @Test
  def findByStatistics {
    val country = Country(id = CountryId(HashHelper.uuid),
                  name = Map("en" -> "name"),
                  phoneCode = "1",
                  pictureFileName = "picture-file-name",
                  statistics = Statistics(1, 1, 1, 1, 1, 1, Seq(StarCategoryId("star_category")), Seq(LocationCategoryId("loc"))))

    service.save(country)

    val expectedResult = Country(id = country.id, name = country.name, phoneCode = country.phoneCode, pictureFileName = null, statistics = null)

    val countriesWithUser = service.findByStatistics(StatisticFields(users = true), fullSearchFields)
    countriesWithUser.contains(expectedResult) must beTrue

    val countriesWithFavorites = service.findByStatistics(StatisticFields(favorites = true), fullSearchFields)
    countriesWithFavorites.contains(expectedResult) must beTrue

    val countriesWithSales = service.findByStatistics(StatisticFields(sales = true), fullSearchFields)
    countriesWithSales.contains(expectedResult) must beTrue

    val countriesWithPromotions = service.findByStatistics(StatisticFields(promotions = true), fullSearchFields)
    countriesWithPromotions.contains(expectedResult) must beTrue

    val countriesWithLocations = service.findByStatistics(StatisticFields(locations = true), fullSearchFields)
    countriesWithLocations.contains(expectedResult) must beTrue

    val countriesWithStarCategory = service.findByStatistics(StatisticFields(starCategory = country.statistics.starCategories.headOption), fullSearchFields)
    countriesWithStarCategory.contains(expectedResult) must beTrue
  }

}

class CountryFixture {
  val country = Country(
    id = CountryId(HashHelper.uuid),
    name = Map("en" -> "name"),
    phoneCode = "1",
    pictureFileName = "picture-file-name"
  )
}