package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationInfoCheckin
import com.tooe.core.domain._
import com.tooe.core.domain.LocationId

trait LocationInfoCheckinConverter extends CoordinatesConverter with MediaObjectConverter with LocationAddressItemConverter {

  import DBObjectConverters._

  implicit val locationInfoCheckinConverter = new DBObjectConverter[LocationInfoCheckin] {

    def serializeObj(obj: LocationInfoCheckin) = DBObjectBuilder()
        .field("lid").value(obj.locationId)
        .field("l").value(obj.coordinates)
        .field("oh").value(obj.openingHours)
        .field("n").value(obj.name)
        .field("a").value(obj.address)
        .field("m").value(obj.media)

    def deserializeObj(source: DBObjectExtractor) =  LocationInfoCheckin(
          locationId = source.field("lid").value[LocationId],
          coordinates = source.field("l").value[Coordinates],
          openingHours = source.field("oh").value[String],
          name = source.field("n").value[String],
          address = source.field("a").value[LocationAddressItem],
          media = source.field("m").opt[MediaObject]
      )

  }

}
