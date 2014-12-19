package com.tooe.core.service

import org.springframework.stereotype.Service
import com.tooe.core.db.mongo.domain.{RegionLocationCategories, User, StatisticFields, Region}
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.RegionRepository
import scala.collection.JavaConverters._
import com.tooe.core.domain.{LocationCategoryId, StarCategoryId, CountryId, RegionId}
import com.tooe.core.usecase.statistics.UpdateRegionOrCountryStatistic
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.db.mongo.domain.promotion.Location
import com.tooe.core.usecase._

trait RegionDataService extends DataService[Region, ObjectId] {

  def findByCountryId(countryId: CountryId): Seq[Region]

  def find(regionId: RegionId): Region

  def updateStatistic(regionId: RegionId, statistic: UpdateRegionOrCountryStatistic): Unit

  def findByStatistics(countryId: CountryId, statisticsFields: StatisticFields): List[Region]

  def findCountryCapital(countryId: CountryId): Option[Region]

  def getRegionLocationCategories(regionId: RegionId): RegionLocationCategories
}

@Service
class RegionDataServiceImpl extends RegionDataService with DataServiceImpl[Region, ObjectId] with StatisticsService[Region, ObjectId]{
  @Autowired var repo: RegionRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Region]

  val fieldsProjection = Map("name" -> "n", "id" -> "_id", "phone" -> "pc")

  def findByCountryId(countryId: CountryId) = repo.findRegionsByCountryId(countryId.id).asScala

  def find(regionId: RegionId): Region = repo.findOne(regionId.id)

  def updateStatistic(regionId: RegionId, statistic: UpdateRegionOrCountryStatistic) {
    incrementStatistic(regionId.id, statistic, mongo, classOf[Region])
  }

  def findByStatistics(countryId: CountryId, statisticsFields: StatisticFields) = {
    val query = Query.query(buildQueryForStatistic(statisticsFields).and("cid").is(countryId.id))
    mongo.find(query, entityClass).asScala.toList
  }

  def findCountryCapital(countryId: CountryId): Option[Region] = {
    val query = new Query(new Criteria("cid").is(countryId.id).and("ic").is(true))
    Option(mongo.findOne(query, entityClass))
  }

  def getRegionLocationCategories(regionId: RegionId): RegionLocationCategories = {
    val region = Option(repo.getRegionLocationCategories(regionId.id))
      .getOrNotFoundException(regionId.id + "not found for location categories in db.region")
    new RegionLocationCategories(region)
  }

  def locationExists(id: LocationCategoryId, on: ObjectId): Boolean = {
    val query = new Query(new Criteria("lc").is(id.id).and("c.a.rid").is(on))
    mongo.exists(query, classOf[Location])
  }
  def starExists(id: StarCategoryId, on: ObjectId): Boolean = {
    val query = new Query(new Criteria("star.sc").is(id.id).and("c.a.rid").is(on))
    mongo.exists(query, classOf[User])
  }
}