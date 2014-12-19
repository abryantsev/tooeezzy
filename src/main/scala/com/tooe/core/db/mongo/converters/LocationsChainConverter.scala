package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{LocationsChainMedia, LocationsChain}
import com.tooe.core.domain.{MediaObject, CompanyId, LocationsChainId}
import com.tooe.core.db.mongo.domain
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import java.util.Date

@WritingConverter
class LocationsChainWriteConverter extends Converter[LocationsChain, DBObject] with LocationsChainConverter {
  def convert(source: LocationsChain) = locationsChainConverter.serialize(source)
}

@ReadingConverter
class LocationsChainReadConverter extends Converter[DBObject, LocationsChain] with LocationsChainConverter {
  def convert(source: DBObject) = locationsChainConverter.deserialize(source)
}

trait LocationsChainConverter extends LocationsChainMediaConverter {

  import DBObjectConverters._
  import DBCommonConverters._

  implicit val locationsChainConverter = new DBObjectConverter[LocationsChain] {
    def serializeObj(obj: LocationsChain) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("d").optObjectMap(obj.description)
      .field("cid").value(obj.companyId)
      .field("t").value(obj.registrationDate)
      .field("lc").value(obj.locationCount)
      .field("cm").value(obj.locationChainMedia)

    def deserializeObj(source: DBObjectExtractor) = domain.LocationsChain(
      id = source.id.value[LocationsChainId],
      name = source.field("n").objectMap[String],
      description = source.field("d").optObjectMap[String],
      companyId = source.field("cid").value[CompanyId],
      registrationDate = source.field("t").value[Date],
      locationCount = source.field("lc").value[Int](default = 0),
      locationChainMedia = source.field("cm").seq[LocationsChainMedia]
    )
  }
}