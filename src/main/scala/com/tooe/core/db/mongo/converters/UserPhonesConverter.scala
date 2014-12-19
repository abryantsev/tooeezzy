package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{Phone, UserPhones}

trait UserPhonesConverter extends UserPhoneConverter{

  implicit val UserPhonesConverter = new DBObjectConverter[UserPhones] {

    def serializeObj(obj: UserPhones) = DBObjectBuilder()
      .field("f").optSeq(obj.all)
      .field("mf").value(obj.main)

    def deserializeObj(source: DBObjectExtractor) = UserPhones(
      all = source.field("f").seqOpt[Phone],
      main = source.field("mf").opt[Phone]
    )
  }
}
