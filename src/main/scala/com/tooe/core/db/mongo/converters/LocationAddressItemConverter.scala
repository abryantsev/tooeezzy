package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.LocationAddressItem

trait LocationAddressItemConverter {

  import DBObjectConverters._

  implicit val locationAddressItemConverter = new DBObjectConverter[LocationAddressItem] {

    def serializeObj(obj: LocationAddressItem) = DBObjectBuilder()
          .field("co").value(obj.country)
          .field("r").value(obj.region)
          .field("s").value(obj.street)

    def deserializeObj(source: DBObjectExtractor) =  LocationAddressItem(
      country = source.field("co").value[String],
      region =  source.field("r").value[String],
      street = source.field("s").value[String]
    )

  }
}
