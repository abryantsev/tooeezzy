package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.CompanyContract
import java.util.Date

trait CompanyContractConverter {

  import DBObjectConverters._

  implicit val companyContractConverter = new DBObjectConverter[CompanyContract] {

    def serializeObj(obj: CompanyContract) = DBObjectBuilder()
      .field("nu").value(obj.number)
      .field("t").value(obj.signingDate)

    def deserializeObj(source: DBObjectExtractor) = CompanyContract(
      number = source.field("nu").value[String],
      signingDate = source.field("t").value[Date]
    )

  }

}
