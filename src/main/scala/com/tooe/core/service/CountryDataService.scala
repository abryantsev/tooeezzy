package com.tooe.core.service

import com.tooe.core.db.mongo.domain.{Location, User, StatisticFields, Country}
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.repository.CountryRepository
import com.tooe.core.domain.{LocationCategoryId, StarCategoryId, CountryField, CountryId}
import scala.collection.JavaConverters._
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.usecase.statistics.UpdateRegionOrCountryStatistic
import org.springframework.data.mongodb.core.query.{Criteria, Query}

trait CountryDataService {
  def find(id: CountryId): Option[Country]
  def findAll(fields: Set[CountryField]): List[Country]
  def save(entity: Country): Country
  def updateStatistic(countryId: CountryId, statistic: UpdateRegionOrCountryStatistic): Unit
  def findByPhone(phone: String, fields: Set[CountryField]): List[Country]
  def findActive(fields: Set[CountryField]): List[Country]
  def findByStatistics(statisticsFields: StatisticFields, fields: Set[CountryField]): List[Country]
}

@Service
class CountryDataServiceImpl extends CountryDataService with StatisticsService[Country, String] {
  @Autowired var repo: CountryRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[Country]

  private val fieldsProjection: Map[CountryField, String] = {
    import CountryField._
    Map(
      Name  -> "n",
      Id    -> "_id",
      Phone -> "pc"
    )
  }

  private def mongoProjection(fields: Set[CountryField]): Seq[String] = fields.map(fieldsProjection).toSeq

  import com.tooe.core.util.ProjectionHelper._

  def find(id: CountryId) = Option(repo.findOne(id.id))

  def save(entity: Country) = repo.save(entity)

  def findAll(fields: Set[CountryField]) =
    mongo.find(new Query().extendProjection(mongoProjection(fields)) ,entityClass).asScala.toList

  def updateStatistic(countryId: CountryId, statistic: UpdateRegionOrCountryStatistic) {
    incrementStatistic(countryId.id, statistic, mongo, classOf[Country])
  }

  def findByPhone(phone: String, fields: Set[CountryField]) = {
    val query = Query.query(new Criteria("pc").is(phone)).extendProjection(mongoProjection(fields))
    mongo.find(query, entityClass).asScala.toList
  }

  def findActive(fields: Set[CountryField]) = {
    val query = Query.query(new Criteria("ia").exists(false)).extendProjection(mongoProjection(fields))
    mongo.find(query, entityClass).asScala.toList
  }

  def findByStatistics(statisticsFields: StatisticFields, fields: Set[CountryField]) = {
    val query = Query.query(buildQueryForStatistic(statisticsFields)).extendProjection(mongoProjection(fields))
    mongo.find(query, entityClass).asScala.toList
  }

  def locationExists(id: LocationCategoryId, on: String): Boolean = {
    val query = new Query(new Criteria("lc").is(id.id).and("c.a.cid").is(on))
    mongo.exists(query, classOf[Location])
    true
  }
  def starExists(id: StarCategoryId, on: String): Boolean = {
    val query = new Query(new Criteria("star.sc").is(id.id).and("c.a.cid").is(on))
    mongo.exists(query, classOf[User])
  }
}