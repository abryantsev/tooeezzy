package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationNewsLike
import com.tooe.core.domain.{LocationNewsLikeId, UserId, LocationNewsId}
import java.util.Date
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class LocationNewsLikeWriteConverter extends Converter[LocationNewsLike, DBObject] with LocationNewsLikeConverter {
  def convert(source: LocationNewsLike) = locationNewsLikeConverter.serialize(source)
}

@ReadingConverter
class LocationNewsLikeReadConverter extends Converter[DBObject, LocationNewsLike] with LocationNewsLikeConverter {
  def convert(source: DBObject) = locationNewsLikeConverter.deserialize(source)
}

trait LocationNewsLikeConverter {

  import DBObjectConverters._

  implicit val locationNewsLikeConverter = new DBObjectConverter[LocationNewsLike] {
    def serializeObj(obj: LocationNewsLike) = DBObjectBuilder()
      .id.value(obj.id)
      .field("lnid").value(obj.locationNewsId)
      .field("t").value(obj.time)
      .field("uid").value(obj.userId)

    def deserializeObj(source: DBObjectExtractor) = LocationNewsLike(
      id = source.id.value[LocationNewsLikeId],
      locationNewsId = source.field("lnid").value[LocationNewsId],
      time = source.field("t").value[Date],
      userId = source.field("uid").value[UserId]
    )
  }


}
