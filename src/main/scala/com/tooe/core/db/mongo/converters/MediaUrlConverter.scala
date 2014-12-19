package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.MediaUrl

trait MediaUrlConverter {
 import DBObjectConverters._
//implicit val stringConverter = new IdentityConverters#IdentityConverters[String]()

  implicit val mediaUrlConverter = new DBObjectConverter[MediaUrl] {

    def serializeObj(obj: MediaUrl) = DBObjectBuilder()
      .field("u").value(obj.imageUrl)

    def deserializeObj(source: DBObjectExtractor) = MediaUrl(source.field("u").value[String])

  }

}
