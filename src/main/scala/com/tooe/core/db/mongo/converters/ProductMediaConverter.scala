package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.ProductMedia
import com.tooe.core.domain.MediaObject

trait ProductMediaConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val productMediaConverter = new DBObjectConverter[ProductMedia] {

    def serializeObj(obj: ProductMedia) = DBObjectBuilder()
      .field("u").value(obj.media)

    def deserializeObj(source: DBObjectExtractor) = ProductMedia(
      media = source.field("u").value[MediaObject]
    )

  }
}
