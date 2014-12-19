package com.tooe.core.db.mongo.converters
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.tooe.core.db.mongo.domain.{LocationsInRegion, LocationsChainStatsId, LocationsChainStats}
import com.mongodb.DBObject
import com.tooe.core.domain.{Coordinates, LocationsChainId, RegionId, CountryId}

@WritingConverter
class LocationsChainStatsWriteConverter extends Converter[LocationsChainStats, DBObject] with LocationsChainStatsConverter {
  def convert(source: LocationsChainStats): DBObject = locationChainStatsConverter.serialize(source)
}

@ReadingConverter
class LocationsChainStatsReadConverter extends Converter[DBObject, LocationsChainStats] with LocationsChainStatsConverter {
  def convert(source: DBObject): LocationsChainStats = locationChainStatsConverter.deserialize(source)
}

trait LocationsChainStatsConverter extends FavoriteStatsConverter with CoordinatesConverter {
  import DBObjectConverters._

  implicit val locationChainStatsConverter = new DBObjectConverter[LocationsChainStats] {
    protected def serializeObj(obj: LocationsChainStats): DBObjectBuilder =
      DBObjectBuilder()
        .id.value(obj.id)
        .field("lcid").value(obj.chainId)
        .field("cid").value(obj.countryId)
        .field("lc").value(obj.locationsCount)
        .field("rs").value(obj.regions)
        .field("mc").value(obj.coordinates)

    protected def deserializeObj(source: DBObjectExtractor): LocationsChainStats = LocationsChainStats(
      id = source.id.value[LocationsChainStatsId],
      chainId = source.field("lcid").value[LocationsChainId],
      countryId = source.field("cid").value[CountryId],
      locationsCount = source.field("lc").value[Int](0),
      regions = source.field("rs").seq[LocationsInRegion],
      coordinates = source.field("mc").value[Coordinates])
  }

  implicit val locationsInRegion: DBSimpleConverter[LocationsInRegion] = new DBObjectConverter[LocationsInRegion] {

    protected def serializeObj(obj: LocationsInRegion): DBObjectBuilder = DBObjectBuilder()
      .field("rid").value(obj.region)
      .field("lc").value(obj.count)

    protected def deserializeObj(source: DBObjectExtractor): LocationsInRegion = LocationsInRegion(
      region = source.field("rid").value[RegionId],
      count = source.field("lc").value[Int](0)
    )

  }
}
