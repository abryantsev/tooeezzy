package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.domain.{LocationPhotoCommentId, LocationPhotoId, UserId}
import java.util.Date
import com.tooe.core.db.mongo.domain.LocationPhotoComment

@WritingConverter
class LocationPhotoCommentWriteConverter extends Converter[LocationPhotoComment, DBObject] with LocationPhotoCommentConverter {

  def convert(source: LocationPhotoComment) = locationPhotoCommentConverter.serialize(source)
}

@ReadingConverter
class LocationPhotoCommentReadConverter extends Converter[DBObject, LocationPhotoComment] with LocationPhotoCommentConverter {

  def convert(source: DBObject) = locationPhotoCommentConverter.deserialize(source)
}

trait LocationPhotoCommentConverter {

  import DBObjectConverters._

  implicit val locationPhotoCommentConverter = new DBObjectConverter[LocationPhotoComment] {
    def serializeObj(obj: LocationPhotoComment) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("pid").value(obj.locationPhotoId)
      .field("t").value(obj.time)
      .field("m").value(obj.message)

    def deserializeObj(source: DBObjectExtractor) = LocationPhotoComment(
      id = source.id.value[LocationPhotoCommentId],
      userId = source.field("uid").value[UserId],
      time = source.field("t").value[Date],
      locationPhotoId = source.field("pid").value[LocationPhotoId],
      message = source.field("m").value[String]
    )
  }
}