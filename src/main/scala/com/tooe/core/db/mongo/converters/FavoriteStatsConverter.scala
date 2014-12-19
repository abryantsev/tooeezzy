package com.tooe.core.db.mongo.converters
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import com.tooe.core.db.mongo.domain.{FavoritesInRegion, FavoriteStatsId, FavoriteStats}
import com.mongodb.DBObject
import org.springframework.core.convert.converter.Converter
import com.tooe.core.domain.{Coordinates, RegionId, CountryId, UserId}

@WritingConverter
class FavoriteStatsWriteConverter extends Converter[FavoriteStats, DBObject] with FavoriteStatsConverter {
  def convert(source: FavoriteStats): DBObject = favoriteStatsConverter.serialize(source)
}

@ReadingConverter
class FavoriteStatsReadConverter extends Converter[DBObject, FavoriteStats] with FavoriteStatsConverter {
  def convert(source: DBObject): FavoriteStats = favoriteStatsConverter.deserialize(source)
}

trait FavoriteStatsConverter extends CoordinatesConverter {
  import DBObjectConverters._

  implicit val favoriteStatsConverter = new DBObjectConverter[FavoriteStats] {
    protected def serializeObj(obj: FavoriteStats): DBObjectBuilder = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("cid").value(obj.countryId)
      .field("fc").value(obj.favoritesCount)
      .field("rs").value(obj.regions)
      .field("mc").value(obj.countryCoordinates)

    protected def deserializeObj(source: DBObjectExtractor): FavoriteStats = FavoriteStats(
      id = source.id.value[FavoriteStatsId],
      userId = source.field("uid").value[UserId],
      countryId = source.field("cid").value[CountryId],
      favoritesCount = source.field("fc").value[Int](0),
      regions = source.field("rs").seq[FavoritesInRegion],
      countryCoordinates = source.field("mc").value[Coordinates])
  }

  implicit val regionsCountConverter: DBSimpleConverter[FavoritesInRegion] = new DBObjectConverter[FavoritesInRegion] {
    protected def serializeObj(obj: FavoritesInRegion): DBObjectBuilder = DBObjectBuilder()
      .field("rid").value(obj.region)
      .field("fc").value(obj.count)

    protected def deserializeObj(source: DBObjectExtractor): FavoritesInRegion = FavoritesInRegion(
      region = source.field("rid").value[RegionId],
      count = source.field("fc").value[Int](0)
    )
  }
}


