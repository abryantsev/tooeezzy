package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.PhoneShort

trait PhotoShortConverter {

  import DBObjectConverters._

  implicit val phoneShortConverter = new DBObjectConverter[PhoneShort] {
    def serializeObj(obj: PhoneShort) = DBObjectBuilder()
      .field("c").value(obj.code)
      .field("n").value(obj.number)

    def deserializeObj(source: DBObjectExtractor) = PhoneShort(
      code = source.field("c").value[String],
      number = source.field("n").value[String]
    )
  }
}