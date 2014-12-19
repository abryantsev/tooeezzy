package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.domain.{LocationPhotoId, LocationPhotoLikeId, UserId}
import java.util.Date
import com.tooe.core.db.mongo.domain.LocationPhotoLike

@WritingConverter
class LocationPhotoLikeWriteConverter extends Converter[LocationPhotoLike, DBObject] with LocationPhotoLikeConverter {

  def convert(source: LocationPhotoLike) = locationPhotoLikeConverter.serialize(source)
}

@ReadingConverter
class LocationPhotoLikeReadConverter extends Converter[DBObject, LocationPhotoLike] with LocationPhotoLikeConverter {

  def convert(source: DBObject) = locationPhotoLikeConverter.deserialize(source)
}

trait LocationPhotoLikeConverter {

  import DBObjectConverters._

  implicit val locationPhotoLikeConverter = new DBObjectConverter[LocationPhotoLike] {
    def serializeObj(obj: LocationPhotoLike) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("pid").value(obj.locationPhotoId)
      .field("t").value(obj.time)

    def deserializeObj(source: DBObjectExtractor) = LocationPhotoLike(
      id = source.id.value[LocationPhotoLikeId],
      userId = source.field("uid").value[UserId],
      time = source.field("t").value[Date],
      locationPhotoId = source.field("pid").value[LocationPhotoId]
    )
  }
}