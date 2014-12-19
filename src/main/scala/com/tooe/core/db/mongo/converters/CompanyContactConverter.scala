package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{Phone, CompanyContact}

trait CompanyContactConverter extends PhoneConverter {

  import DBObjectConverters._

  implicit val companyContactConverter = new DBObjectConverter[CompanyContact] {

    def serializeObj(obj: CompanyContact) = DBObjectBuilder()
      .field("p").value(obj.phones)
      .field("a").value(obj.address)
      .field("la").value(obj.legalAddress)
      .field("url").value(obj.url)

    def deserializeObj(source: DBObjectExtractor) = CompanyContact(
      phones = source.field("p").seq[Phone],
      address = source.field("a").value[String],
      legalAddress = source.field("la").value[String],
      url = source.field("url").opt[String]
    )

  }

}
