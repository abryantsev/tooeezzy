package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.{UrlType, MediaObjectId, MediaObject}

trait MediaObjectConverter {
  import DBObjectConverters._

  implicit val mediaObjectConverter = new DBObjectConverter[MediaObject] {

    def serializeObj(obj: MediaObject) = DBObjectBuilder()
      .field("mu").value(obj.url)
      .field("t").value(obj.mediaType)

    def deserializeObj(source: DBObjectExtractor) =
      MediaObject(source.field("mu").value[MediaObjectId], source.field("t").opt[UrlType])

  }
}
