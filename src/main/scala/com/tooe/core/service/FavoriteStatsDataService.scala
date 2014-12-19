package com.tooe.core.service
import com.tooe.core.db.mongo.domain._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.{Criteria, Query}
import com.tooe.core.domain.{CountryId, UserId}
import scala.collection.convert._
import com.tooe.core.db.mongo.repository.FavoriteStatsRepository

trait FavoriteStatsDataService {
  def findOne(id: FavoriteStatsId): Option[FavoriteStats]
  def save(stats: FavoriteStats): FavoriteStats
  def delete(id: FavoriteStatsId): Unit
  def findByUserId(uid: UserId): Seq[FavoriteStats]
  def mergeStatsAndSave(fs: FavoriteStats): FavoriteStats
  def findByUserAndCountry(uid: UserId, cid: CountryId): Option[FavoriteStats]
}

@Service
class FavoriteStatsDataServiceImpl extends FavoriteStatsDataService {
  @Autowired var repo: FavoriteStatsRepository = _
  @Autowired var mongo: MongoTemplate = _

  val entityClass = classOf[FavoriteStats]

  def findOne(id: FavoriteStatsId): Option[FavoriteStats] = Option(repo.findOne(id.id))

  def save(stats: FavoriteStats): FavoriteStats = repo.save(stats)

  def delete(id: FavoriteStatsId): Unit = repo.delete(id.id)

  def findByUserId(uid: UserId): Seq[FavoriteStats] = {
    val query = Query.query(new Criteria("uid").is(uid.id))
    WrapAsScala.asScalaBuffer(mongo.find(query, entityClass))
  }

  def findByUserAndCountry(uid: UserId, cid: CountryId): Option[FavoriteStats] =
    Option(mongo.findOne(Query.query(new Criteria("uid").is(uid.id).and("cid").is(cid.id)), entityClass))

  def mergeStatsAndSave(stats: FavoriteStats): FavoriteStats = {
    val query = Query.query(new Criteria("uid").is(stats.userId.id).and("cid").is(stats.countryId.id))
    Option(mongo.findOne(query, entityClass)).map {
      found => //case it exists
        val updated = mergeStats(found, stats)
        if (updated.favoritesCount > 0)
          save(updated)
        else delete(updated.id)
        updated
    } getOrElse {
      if (stats.regions.length > 0 && stats.favoritesCount > 0) save(stats)
      stats
    }
  }

  private def mergeStats(target: FavoriteStats, merge: FavoriteStats): FavoriteStats = {
    val regions = target.regions ++ merge.regions
    val mergedRegions = regions.groupBy(_.region).mapValues(v => v.foldLeft(0)(_ + _.count))
      .withFilter(_._2 > 0).map(x => FavoritesInRegion(x._1, x._2)).toSeq
    target.copy(regions = mergedRegions, favoritesCount = target.favoritesCount + merge.favoritesCount)
  }
}