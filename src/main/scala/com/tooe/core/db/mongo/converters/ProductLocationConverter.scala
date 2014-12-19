package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationWithName
import com.tooe.core.domain.LocationId

trait ProductLocationConverter {

  import DBObjectConverters._

  implicit val productLocationConverter = new DBObjectConverter[LocationWithName] {
    def serializeObj(obj: LocationWithName) = DBObjectBuilder()
      .field("lid").value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = LocationWithName(
      id = source.field("lid").value[LocationId],
      name = source.field("n").objectMap[String]
    )
  }

}
