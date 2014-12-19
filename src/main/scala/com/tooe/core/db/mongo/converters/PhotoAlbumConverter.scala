package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.PhotoAlbum
import com.tooe.core.domain.{MediaObject, UserId, PhotoAlbumId}
import java.util.Date
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class PhotoAlbumWriteConverter extends Converter[PhotoAlbum, DBObject] with PhotoAlbumConverter {
  def convert(source: PhotoAlbum) = photoAlbumConverter.serialize(source)
}

@ReadingConverter
class PhotoAlbumReadConverter extends Converter[DBObject, PhotoAlbum] with PhotoAlbumConverter {
  def convert(source: DBObject) = photoAlbumConverter.deserialize(source)
}

trait PhotoAlbumConverter extends MediaObjectConverter{
  import DBObjectConverters._

  implicit val photoAlbumConverter = new DBObjectConverter[PhotoAlbum] {
    def serializeObj(obj: PhotoAlbum) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("n").value(obj.name)
      .field("d").value(obj.description)
      .field("c").value(obj.count)
      .field("p").value(obj.frontPhotoUrl)
      .field("av").value(obj.allowedView)
      .field("ac").value(obj.allowedComment)
      .field("t").value(obj.createdTime)
      .field("de").value(obj.default)

    def deserializeObj(source: DBObjectExtractor) = PhotoAlbum(
      id = source.id.value[PhotoAlbumId],
      userId = source.field("uid").value[UserId],
      name = source.field("n").value[String],
      description = source.field("d").opt[String],
      count = source.field("c").value[Int](default = 0),
      frontPhotoUrl = source.field("p").value[MediaObject],
      allowedView = source.field("av").seq[String],
      allowedComment = source.field("ac").seq[String],
      createdTime = source.field("t").value[Date],
      default = source.field("de").opt[Boolean]
    )
  }
}
