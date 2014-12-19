package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.domain.{MediaObject, PhotoId, UserId, PhotoAlbumId}
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import java.util.Date

@WritingConverter
class PhotoWriteConverter extends Converter[Photo, DBObject] with PhotoConverter {
  def convert(source: Photo) = photoConverter.serialize(source)
}

@ReadingConverter
class PhotoReadConverter extends Converter[DBObject, Photo] with PhotoConverter {
  def convert(source: DBObject) = photoConverter.deserialize(source)
}

trait PhotoConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val photoConverter = new DBObjectConverter[Photo] {
    def serializeObj(obj: Photo) = DBObjectBuilder()
      .id.value(obj.id)
      .field("pa").value(obj.photoAlbumId)
      .field("uid").value(obj.userId)
      .field("n").value(obj.name)
      .field("u").value(obj.fileUrl)
      .field("lc").value(obj.likesCount)
      .field("ls").value(obj.usersLikesIds)
      .field("cc").value(obj.commentsCount)
      .field("cu").value(obj.usersCommentsIds)
      .field("t").value(obj.createdAt)

    def deserializeObj(source: DBObjectExtractor) = Photo(
      id = source.id.value[PhotoId],
      userId = source.field("uid").value[UserId],
      name = source.field("n").opt[String],
      photoAlbumId = source.field("pa").value[PhotoAlbumId],
      likesCount = source.field("lc").value[Int](default = 0),
      fileUrl = source.field("u").value[MediaObject],
      usersLikesIds = source.field("ls").seq[UserId],
      commentsCount = source.field("cc").value[Int](default = 0),
      usersCommentsIds = source.field("cu").seq[UserId],
      createdAt = source.field("t").value[Date]
    )
  }
}
