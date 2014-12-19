package com.tooe.core.service
import org.springframework.beans.factory.annotation.Autowired
import com.tooe.core.db.mongo.converters.MongoDaoHelper
import org.junit.Test
import com.tooe.core.db.mongo.domain.{LocationsInRegion, LocationsChainStats}
import com.tooe.core.domain.{Coordinates, LocationsChainId, RegionId, CountryId}
import scala.util.Random

class LocationsChainStatsDataServiceTest extends SpringDataMongoTestHelper {
  @Autowired var service: LocationsChainStatsDataServiceImpl = _

  val entities = new MongoDaoHelper("locationschain_stats")
  val fixture = new LocationsChainStatsFixture

  @Test
  def mateFeedKillRepeat {
    val entity = fixture.genEntity
    service.findOne(entity.id) === None
    service.save(entity) === entity
    service.findOne(entity.id) === entity
    service.delete(entity.id)
    service.findOne(entity.id) === None
  }

  @Test
  def updateOrInsert {
    val mainEntity = fixture.genEntity
    val entities = (1 to 10).map(_ => mainEntity)
    entities.foreach(x => service.mergeStatsAndSave(x))

    val updated = service.findOne(mainEntity.id).get
    updated.locationsCount === 20
    updated.regions.length === 2
    updated.regions.forall(_.count == 5) === true
  }

  @Test
  def representation {
    val entity = fixture.genEntity
    service.save(entity)
    val repr = entities.findOne(entity.id.id)
    jsonAssert(repr)(
      s"""{
  _id : {"$$oid" : "${entity.id.id.toString}"},
  lcid : {"$$oid" : "${entity.chainId}"},
  cid: {"$$oid" : "${entity.countryId.id}"},
  lc: ${entity.locationsCount},
  rs: [{ rid: {"$$oid" : "${entity.regions.head.region.toString}"}, lc: 1}, { rid: {"$$oid" : "${entity.regions.tail.head.region.toString}"}, lc: 1}],
  mc: { lng : ${entity.coordinates.longitude}, lat: ${entity.coordinates.latitude} }
    }""")
    service.delete(entity.id)
  }
}

class LocationsChainStatsFixture {

  def genEntity = LocationsChainStats(
    chainId = LocationsChainId(),
    countryId = CountryId("noo york"),
    locationsCount = 2,
    regions = Seq(LocationsInRegion(RegionId(), 1), LocationsInRegion(RegionId(), 1)),
    coordinates = Coordinates(Random.nextDouble(), Random.nextDouble()))
}
