package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.Phone

object PhoneConverter extends PhoneConverter

trait PhoneConverter {
  import DBObjectConverters._

  implicit val PhoneConverter = new DBObjectConverter[Phone] {
    def serializeObj(obj: Phone) = DBObjectBuilder()
      .field("c").value(obj.countryCode)
      .field("n").value(obj.number)
      .field("p").value(obj.purpose)

    def deserializeObj(source: DBObjectExtractor) = Phone(
      countryCode = source.field("c").value[String],
      number = source.field("n").value[String],
      purpose = source.field("p").opt[String]
    )
  }
}