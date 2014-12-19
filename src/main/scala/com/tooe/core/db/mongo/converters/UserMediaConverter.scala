package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.UserMedia
import com.tooe.core.domain.MediaObject

trait UserMediaConverter extends MediaObjectConverter {
  import DBObjectConverters._

  implicit val UserMediaConverter = new DBObjectConverter[UserMedia] {
    def serializeObj(obj: UserMedia) = DBObjectBuilder()
      .field("u").value(obj.url)
      .field("d").value(obj.description)
      .field("t").value(obj.mediaType)
      .field("p").value(obj.purpose)
      .field("f").value(obj.videoFormat)
      .field("ds").value(obj.descriptionStyle)
      .field("dc").value(obj.descriptionColor)

    def deserializeObj(source: DBObjectExtractor) = UserMedia(
      url = source.field("u").value[MediaObject],
      description = source.field("d").opt[String],
      mediaType = source.field("t").value[String],
      purpose = source.field("p").opt[String],
      videoFormat = source.field("f").opt[String],
      descriptionStyle = source.field("ds").opt[String],
      descriptionColor = source.field("dc").opt[String]
    )
  }
}