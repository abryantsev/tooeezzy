package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.UserDetails
import java.util.Date
import com.tooe.core.domain.CurrencyId

trait UserDetailsConverter {
  import DBObjectConverters._

  implicit val UserDetailsConverter = new DBObjectConverter[UserDetails] {
    def serializeObj(obj: UserDetails) = DBObjectBuilder()
      .field("cu").value(obj.defaultCurrency)
      .field("rt").value(obj.registrationTime)
      .field("e").value(obj.education)
      .field("job").value(obj.job)
      .field("am").value(obj.aboutMe)

    def deserializeObj(source: DBObjectExtractor) = UserDetails(
      defaultCurrency = source.field("cu").value[CurrencyId],
      registrationTime = source.field("rt").value[Date],
      education = source.field("e").opt[String],
      job = source.field("job").opt[String],
      aboutMe = source.field("am").opt[String]
    )
  }
}