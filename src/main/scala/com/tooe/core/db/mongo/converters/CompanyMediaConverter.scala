package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.CompanyMedia
import com.tooe.core.domain.MediaObject

trait CompanyMediaConverter extends MediaObjectConverter {

  import DBObjectConverters._

  implicit val companyMediaConverter = new DBObjectConverter[CompanyMedia] {

    def serializeObj(obj: CompanyMedia) = DBObjectBuilder()
      .field("p").value(obj.purpose)
      .field("u").value(obj.url)

    def deserializeObj(source: DBObjectExtractor) = CompanyMedia(
      purpose = source.field("p").opt[String],
      url = source.field("u").value[MediaObject]
    )

  }
}
