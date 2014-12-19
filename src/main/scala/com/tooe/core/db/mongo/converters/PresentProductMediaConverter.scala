package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.PresentProductMedia
import com.tooe.core.domain.MediaObject

trait PresentProductMediaConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val presentProductMediaConverter = new DBObjectConverter[PresentProductMedia] {
    def serializeObj(obj: PresentProductMedia) = DBObjectBuilder()
      .field("u").value(obj.url)
      .field("d").value(obj.description)
      
    def deserializeObj(source: DBObjectExtractor) = PresentProductMedia(
      url = source.field("u").value[MediaObject],
      description = source.field("d").opt[String]
    )
  }
}