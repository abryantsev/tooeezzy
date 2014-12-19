package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{PaymentLimit, CompanyPayment}
import com.tooe.core.domain.PaymentPeriod

trait PaymentCompanyConverter extends PaymentLimitConverter {
  import DBObjectConverters._

  implicit val paymentCompanyConverter = new DBObjectConverter[CompanyPayment] {
    def serializeObj(obj: CompanyPayment) = DBObjectBuilder()
      .field("ba").value(obj.bankAccount)
      .field("bi").value(obj.bik)
      .field("bt").value(obj.transferAccount)
      .field("p").value(obj.frequency)
      .field("l").value(obj.limit)

    def deserializeObj(source: DBObjectExtractor) = CompanyPayment(
      bankAccount = source.field("ba").value[String],
      bik = source.field("bi").value[String],
      transferAccount = source.field("bt").value[String],
      frequency = source.field("p").opt[PaymentPeriod],
      limit = source.field("l").opt[PaymentLimit]
    )
  }
}
