package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.UserAddress
import com.tooe.core.domain.{CountryId, RegionId}

trait UserAddressConverter {
  import DBObjectConverters._

  implicit val UserAddressConverter = new DBObjectConverter[UserAddress] {

    def serializeObj(obj: UserAddress) = DBObjectBuilder()
      .field("cid").value(obj.countryId)
      .field("rid").value(obj.regionId)
      .field("r").value(obj.regionName)
      .field("co").value(obj.country)

    def deserializeObj(source: DBObjectExtractor) = UserAddress(
      countryId = source.field("cid").value[CountryId],
      regionId = source.field("rid").value[RegionId],
      regionName = source.field("r").value[String],
      country = source.field("co").value[String]
    )
  }
}
