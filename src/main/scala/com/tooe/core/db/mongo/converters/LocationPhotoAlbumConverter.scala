package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.{MediaObject, LocationsChainId, LocationId, LocationPhotoAlbumId}
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class LocationPhotoAlbumWriteConverter extends Converter[LocationPhotoAlbum, DBObject] with LocationPhotoAlbumConverter {
  def convert(source: LocationPhotoAlbum) = locationPhotoAlbumConverter.serialize(source)
}

@ReadingConverter
class LocationPhotoAlbumReadConverter extends Converter[DBObject, LocationPhotoAlbum] with LocationPhotoAlbumConverter {
  def convert(source: DBObject) = locationPhotoAlbumConverter.deserialize(source)
}

trait LocationPhotoAlbumConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val locationPhotoAlbumConverter = new DBObjectConverter[LocationPhotoAlbum] {
    def serializeObj(obj: LocationPhotoAlbum) = DBObjectBuilder()
      .id.value(obj.id)
      .field("lid").value(obj.locationId)
      .field("n").value(obj.name)
      .field("d").value(obj.description)
      .field("c").value(obj.photosCount)
      .field("p").value(obj.frontPhotoUrl)
      .field("lcid").value(obj.locationsChainId)

    def deserializeObj(source: DBObjectExtractor) = LocationPhotoAlbum(
      id = source.id.value[LocationPhotoAlbumId],
      locationId = source.field("lid").value[LocationId],
      name = source.field("n").value[String],
      description = source.field("d").opt[String],
      photosCount = source.field("c").value[Int](default = 0),
      frontPhotoUrl = source.field("p").value[MediaObject],
      locationsChainId = source.field("lcid").opt[LocationsChainId]
    )
  }
}