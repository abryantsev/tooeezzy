package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationCategoryMedia

trait LocationCategoryMediaConverter {

  import DBObjectConverters._

  implicit val LocationCategoryMediaConverter = new DBObjectConverter[LocationCategoryMedia] {
    def serializeObj(obj: LocationCategoryMedia) = DBObjectBuilder()
      .field("u").value(obj.url)
      .field("p").value(obj.purpose)

    def deserializeObj(source: DBObjectExtractor) = LocationCategoryMedia(
      url = source.field("u").value[String],
      purpose = source.field("p").opt[String]
    )
  }
}