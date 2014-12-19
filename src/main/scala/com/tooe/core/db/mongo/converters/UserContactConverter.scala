package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{UserPhones, Phone, UserAddress, UserContact}

trait UserContactConverter extends UserAddressConverter with UserPhonesConverter {
  import DBObjectConverters._

  implicit val UserContactConverter = new DBObjectConverter[UserContact] {
    def serializeObj(obj: UserContact) = DBObjectBuilder()
      .field("a").value(obj.address)
      .field("p").value(obj.phones)
      .field("e").value(obj.email)

    def deserializeObj(source: DBObjectExtractor) = UserContact(
      address = source.field("a").value[UserAddress],
      phones = source.field("p").value[UserPhones],
      email = source.field("e").value[String]
    )
  }
}