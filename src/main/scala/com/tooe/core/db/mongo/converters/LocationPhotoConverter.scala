package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.domain.MediaObject
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.LocationPhoto
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.domain.LocationPhotoId

@WritingConverter
class LocationPhotoWriteConverter extends Converter[LocationPhoto, DBObject] with LocationPhotoConverter {
  def convert(source: LocationPhoto) = locationPhotoConverter.serialize(source)
}

@ReadingConverter
class LocationPhotoReadConverter extends Converter[DBObject, LocationPhoto] with LocationPhotoConverter {
  def convert(source: DBObject) = locationPhotoConverter.deserialize(source)
}

trait LocationPhotoConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val locationPhotoConverter = new DBObjectConverter[LocationPhoto] {
    def serializeObj(obj: LocationPhoto) = DBObjectBuilder()
      .id.value(obj.id)
      .field("pid").value(obj.photoAlbumId)
      .field("lid").value(obj.locationId)
      .field("n").value(obj.name)
      .field("t").value(obj.creationDate)
      .field("u").value(obj.fileUrl)
      .field("lc").value(obj.likesCount)
      .field("ls").value(obj.usersLikesIds)
      .field("cc").value(obj.commentsCount)
      .field("cu").value(obj.comments)

    def deserializeObj(source: DBObjectExtractor) = LocationPhoto(
      id = source.id.value[LocationPhotoId],
      photoAlbumId = source.field("pid").value[LocationPhotoAlbumId],
      locationId = source.field("lid").value[LocationId],
      name = source.field("n").opt[String],
      creationDate = source.field("t").value[Date],
      fileUrl = source.field("u").value[MediaObject],
      likesCount = source.field("lc").value[Int](default = 0),
      usersLikesIds = source.field("ls").seq[UserId],
      commentsCount = source.field("cc").value[Int](default = 0),
      comments = source.field("cu").seq[UserId]
    )
  }

}
