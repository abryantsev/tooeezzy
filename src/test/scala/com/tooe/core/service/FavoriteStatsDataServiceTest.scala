package com.tooe.core.service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.db.mongo.domain.{FavoritesInRegion, FavoriteStats}
import com.tooe.core.domain._
import scala.util.Random

class FavoriteStatsDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: FavoriteStatsDataServiceImpl = _

  val entities = new MongoDaoHelper("favorites_stats")
  val fixture = new FavoriteStatsFixture

  @Test
  def mateFeedKillRepeat {
    val entity = fixture.genEntity
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === Some(entity)
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def updateOrInsert {
    val mainEntity = fixture.genEntity
    val entities = (1 to 10).map(_ => mainEntity)
    entities.foreach(x => service.mergeStatsAndSave(x))

    val updated = service.findOne(mainEntity.id).get
    updated.favoritesCount === 20
    updated.regions.length === 2
    updated.regions.forall(_.count == 10) === true
  }

  @Test
  def representation {
    val entity = fixture.genEntity
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(
    s"""{
      _id : {"$$oid" : "${entity.id.id.toString}"},
      uid : {"$$oid" : "${entity.userId.id.toString}"},
      cid: "${entity.countryId.id}",
      fc: ${entity.favoritesCount},
      rs: [{ rid: ${entity.regions.head.region.id.mongoRepr}, fc: 1}, { rid: ${entity.regions.last.region.id.mongoRepr}, fc: 1}],
      mc: { lon : ${entity.countryCoordinates.longitude}, lat: ${entity.countryCoordinates.latitude} }
    }""")
    service.delete(entity.id)
  }
}

class FavoriteStatsFixture {

  def genEntity = FavoriteStats(
    userId = UserId(),
    countryId = CountryId("noo york"),
    favoritesCount = 2,
    regions = Seq(FavoritesInRegion(RegionId(), 1),FavoritesInRegion(RegionId(), 1)),
    countryCoordinates = Coordinates(Random.nextDouble(), Random.nextDouble()) //outer space
  )
}
