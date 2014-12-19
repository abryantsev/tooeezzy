package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationAddress
import com.tooe.core.domain.{CountryId, RegionId, Coordinates}

trait LocationAddressConverter extends CoordinatesConverter {

  import DBObjectConverters._

  implicit val locationAddressConverter = new DBObjectConverter[LocationAddress] {

    def serializeObj(obj: LocationAddress) = DBObjectBuilder()
      .field("l").value(obj.coordinates)
      .field("rid").value(obj.regionId)
      .field("r").value(obj.regionName)
      .field("cid").value(obj.countryId)
      .field("co").value(obj.country)
      .field("s").value(obj.street)

    def deserializeObj(source: DBObjectExtractor) = LocationAddress(
      coordinates = source.field("l").value[Coordinates], //asCoordinates(source.asDBObject("l")),
      regionId =  source.field("rid").value[RegionId],
      regionName = source.field("r").value[String],
      countryId = source.field("cid").value[CountryId],
      country = source.field("co").value[String],
      street = source.field("s").value[String]
    )
  }

}
