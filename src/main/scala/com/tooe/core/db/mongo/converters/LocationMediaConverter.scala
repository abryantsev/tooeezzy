package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationMedia
import com.tooe.core.domain.MediaObject

trait LocationMediaConverter extends MediaObjectConverter {
  import DBObjectConverters._

  implicit val LocationMediaConverter = new DBObjectConverter[LocationMedia] {
    def serializeObj(obj: LocationMedia) = DBObjectBuilder()
      .field("u").value(obj.url)
      .field("d").value(obj.description)
      .field("t").value(obj.mediaType)
      .field("p").value(obj.purpose)

    def deserializeObj(source: DBObjectExtractor) = LocationMedia(
      url = source.field("u").value[MediaObject],
      description = source.field("d").opt[String],
      mediaType = source.field("t").value[String],
      purpose = source.field("p").opt[String]
    )
  }
}