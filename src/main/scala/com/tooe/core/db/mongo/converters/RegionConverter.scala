package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import org.bson.types.ObjectId
import com.tooe.core.domain.{Coordinates, RegionId, CountryId}

@WritingConverter
class RegionWriteConverter extends Converter[Region, DBObject] with RegionConverter {

  def convert(source: Region): DBObject = regionConverter.serialize(source)
}


@ReadingConverter
class RegionReadConverter extends Converter[DBObject, Region] with RegionConverter {

  def convert(source: DBObject): Region = regionConverter.deserialize(source)
}

trait RegionConverter extends StatisticsConverter with CoordinatesConverter{

  import DBObjectConverters._

  implicit val regionConverter = new DBObjectConverter[Region] {
    def serializeObj(obj: Region) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("cid").value(obj.countryId)
      .field("ic").value(obj.isCapital)
      .field("c").value(obj.coordinates)
      .field("st").value(obj.statistics)

    def deserializeObj(source: DBObjectExtractor) = Region(
      id = source.id.value[RegionId],
      name = source.field("n").objectMap[String],
      countryId = source.field("cid").value[CountryId],
      isCapital = source.field("ic").opt[Boolean],
      coordinates = source.field("c").value[Coordinates],
      statistics = source.field("st").value[Statistics]
    )
  }
}