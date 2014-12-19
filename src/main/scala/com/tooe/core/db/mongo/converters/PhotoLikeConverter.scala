package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.PhotoLike
import com.tooe.core.domain.{UserId, PhotoId, PhotoLikeId}
import java.util.Date

@WritingConverter
class PhotoLikeWriteConverter extends Converter[PhotoLike, DBObject] with PhotoLikeConverter {
  def convert(obj: PhotoLike) = photoLikeConverter.serialize(obj)
}

@ReadingConverter
class PhotoLikeReadConverter extends Converter[DBObject, PhotoLike] with PhotoLikeConverter {
  def convert(source: DBObject) = photoLikeConverter.deserialize(source)
}


trait PhotoLikeConverter {

  import DBObjectConverters._

  implicit val photoLikeConverter = new DBObjectConverter[PhotoLike] {

    def serializeObj(obj: PhotoLike) = DBObjectBuilder()
      .id.value(obj.id)
      .field("pid").value(obj.photoId)
      .field("t").value(obj.time)
      .field("uid").value(obj.userId)

    def deserializeObj(source: DBObjectExtractor) = PhotoLike(
      id = source.id.value[PhotoLikeId],
      photoId = source.field("pid").value[PhotoId],
      time = source.field("t").value[Date],
      userId = source.field("uid").value[UserId]
     )
  }

}