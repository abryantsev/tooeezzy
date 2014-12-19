package com.tooe.core.service
import com.tooe.core.db.mongo.domain._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.domain.{CountryId, LocationsChainId}
import scala.collection.convert.WrapAsScala

trait LocationsChainStatsDataService {
  def findOne(id: LocationsChainStatsId): Option[LocationsChainStats]
  def findByChain(id: LocationsChainId): Seq[LocationsChainStats]
  def mergeStatsAndSave(stats: LocationsChainStats): LocationsChainStats
  def findByChainAndCountry(chain: LocationsChainId, country: CountryId): LocationsChainStats
}

@Service
class LocationsChainStatsDataServiceImpl extends LocationsChainStatsDataService {
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[LocationsChainStats]

  def findOne(id: LocationsChainStatsId): Option[LocationsChainStats] =
    Option {
      mongo.findById(id.id, entityClass)
    }

  private[service] def save(stats: LocationsChainStats): LocationsChainStats = {
    mongo.save(stats)
    stats
  }

  private[service] def delete(id: LocationsChainStatsId): Unit = {
    mongo.remove(Query.query(new Criteria("_id").is(id.id)), entityClass)
  }

  def findByChain(id: LocationsChainId): Seq[LocationsChainStats] = {
    val query = Query.query(new Criteria("lcid").is(id.id))
    WrapAsScala.asScalaBuffer(mongo.find(query, entityClass))
  }

  def findByChainAndCountry(chain: LocationsChainId, country: CountryId): LocationsChainStats = {
    val query = Query.query(new Criteria("lcid").is(chain.id).and("cid").is(country.id))
    Option(mongo.findOne(query, entityClass)).getOrElse {
      throw new Exception("there are no locations of this chain in this country.")
    }
  }

  def mergeStatsAndSave(stats: LocationsChainStats): LocationsChainStats = {
    val query = Query.query(new Criteria("lcid").is(stats.chainId.id).and("cid").is(stats.countryId.id))
    Option(mongo.findOne(query, entityClass)).map {
      found => //case it exists
        val updated = mergeStats(found, stats)
        if (updated.locationsCount > 0)
          save(updated)
        else delete(updated.id)
        updated
    }.getOrElse {
      if (stats.regions.length > 0 && stats.locationsCount > 0) save(stats)
      stats
    }
  }

  private def mergeStats(target: LocationsChainStats, merge: LocationsChainStats): LocationsChainStats = {
    val regions = target.regions ++ merge.regions
    val mergedRegions = regions.groupBy(_.region).mapValues(v => v.foldLeft(0)(_ + _.count))
      .withFilter(_._2 > 0).map(x => LocationsInRegion(x._1, x._2)).toSeq
    target.copy(regions = mergedRegions, locationsCount = target.locationsCount + merge.locationsCount)
  }
}
