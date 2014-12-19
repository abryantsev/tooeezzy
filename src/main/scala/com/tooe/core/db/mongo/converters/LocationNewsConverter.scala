package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.LocationNews
import com.tooe.core.domain.{LocationsChainId, UserId, LocationId, LocationNewsId}
import java.util.Date

@WritingConverter
class LocationNewsWriteConverter extends Converter[LocationNews, DBObject] with LocationNewsConverter {
  def convert(source: LocationNews) = locationNewsConverter.serialize(source)
}

@ReadingConverter
class LocationNewsReadConverter extends Converter[DBObject, LocationNews] with LocationNewsConverter {
  def convert(source: DBObject) = locationNewsConverter.deserialize(source)
}

trait LocationNewsConverter {

  import DBObjectConverters._

  implicit val locationNewsConverter = new DBObjectConverter[LocationNews] {
    def serializeObj(obj: LocationNews) = DBObjectBuilder()
      .id.value(obj.id)
      .field("c").value(obj.content)
      .field("cf").value(obj.commentsEnabled)
      .field("t").value(obj.createdTime)
      .field("lid").value(obj.locationId)
      .field("lc").value(obj.likesCount)
      .field("ls").value(obj.lastLikes)
      .field("lcid").value(obj.locationsChainId)

    def deserializeObj(source: DBObjectExtractor) = LocationNews(
      id = source.id.value[LocationNewsId],
      content = source.field("c").objectMap[String],
      commentsEnabled = source.field("cf").opt[Boolean],
      createdTime = source.field("t").value[Date],
      locationId = source.field("lid").value[LocationId],
      likesCount = source.field("lc").value[Int](0),
      lastLikes = source.field("ls").seq[UserId],
      locationsChainId = source.field("lcid").opt[LocationsChainId]
    )
  }
}