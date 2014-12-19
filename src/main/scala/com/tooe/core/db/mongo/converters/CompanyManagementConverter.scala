package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.CompanyManagement

trait CompanyManagementConverter {

  import DBObjectConverters._

  implicit val companyManagementConverter = new DBObjectConverter[CompanyManagement] {

    def serializeObj(obj: CompanyManagement) = DBObjectBuilder()
      .field("on").value(obj.officeName)
      .field("mn").value(obj.managerName)
      .field("an").value(obj.accountantName)

    def deserializeObj(source: DBObjectExtractor) = CompanyManagement(
      officeName = source.field("on").value[String],
      managerName = source.field("mn").value[String],
      accountantName = source.field("an").value[String]
    )

  }


}
