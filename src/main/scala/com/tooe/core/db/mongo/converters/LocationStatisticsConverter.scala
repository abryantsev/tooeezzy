package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationStatistics
import com.tooe.core.domain.{LocationId, LocationStatisticsId}
import com.tooe.core.db.mongo.domain
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import java.util.Date

@WritingConverter
class LocationStatisticsWriteConverter extends Converter[LocationStatistics, DBObject] with LocationStatisticsConverter {
  def convert(source: LocationStatistics) = locationStatisticsConverter.serialize(source)
}

@ReadingConverter
class LocationStatisticsReadConverter extends Converter[DBObject, LocationStatistics] with LocationStatisticsConverter {
  def convert(source: DBObject) = locationStatisticsConverter.deserialize(source)
}

trait LocationStatisticsConverter {

  import DBObjectConverters._

  implicit val locationStatisticsConverter = new DBObjectConverter[LocationStatistics] {
    def serializeObj(obj: LocationStatistics) = DBObjectBuilder()
      .id.value(obj.id)
      .field("lid").value(obj.locationId)
      .field("rt").value(obj.registrationDate)
      .field("vc").value(obj.visitorsCount)

    def deserializeObj(source: DBObjectExtractor) = domain.LocationStatistics(
      id = source.id.value[LocationStatisticsId],
      locationId = source.field("lid").value[LocationId],
      registrationDate = source.field("rt").value[Date],
      visitorsCount= source.field("vc").value[Int](0)
    )
  }

}
