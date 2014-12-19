package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{Phone, LocationAddress, LocationContact}

trait LocationContactConverter extends LocationAddressConverter with PhoneConverter {

  import DBObjectConverters._

  implicit val locationContactConverter = new DBObjectConverter[LocationContact] {

    def serializeObj(obj: LocationContact) = DBObjectBuilder()
      .field("a").value(obj.address)
      .field("p").value(obj.phones)
      .field("url").value(obj.url)

    def deserializeObj(source: DBObjectExtractor) = LocationContact(
      address = source.field("a").value[LocationAddress],
      phones = source.field("p").seq[Phone],
      url = source.field("url").opt[String]
    )

  }
}
