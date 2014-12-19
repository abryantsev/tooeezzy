package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.CompanyInformation
import java.util.Date
import com.tooe.core.domain.CompanyType

trait CompanyInformationConverter {

  import DBObjectConverters._

  implicit val companyInformationConverter = new DBObjectConverter[CompanyInformation] {

    def serializeObj(obj: CompanyInformation) = DBObjectBuilder()
      .field("cs").value(obj.companyStructure)
      .field("ogrn").value(obj.ogrn)
      .field("lrt").value(obj.registrationDate)
      .field("ri").value(obj.registrator)
      .field("tn").value(obj.taxNumber)
      .field("kpp").value(obj.kpp)
      .field("li").value(obj.licenceInformation)
      .field("lt").value(obj.companyType)

    def deserializeObj(source: DBObjectExtractor) = CompanyInformation(
      companyStructure = source.field("cs").value[String],
      ogrn = source.field("ogrn").opt[String],
      registrationDate = source.field("lrt").opt[Date],
      registrator = source.field("ri").opt[String],
      taxNumber = source.field("tn").value[String],
      kpp = source.field("kpp").opt[String],
      licenceInformation = source.field("li").opt[String],
      companyType = source.field("lt").value[CompanyType]
    )

  }

}
