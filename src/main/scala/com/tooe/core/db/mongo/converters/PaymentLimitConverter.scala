package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.PaymentLimit
import com.tooe.core.domain.CurrencyId

trait PaymentLimitConverter {
  import DBObjectConverters._

  implicit val paymentLimitConverter = new DBObjectConverter[PaymentLimit] {
    def serializeObj(obj: PaymentLimit) = DBObjectBuilder()
      .field("v").value(obj.value)
      .field("c").value(obj.currency)

    def deserializeObj(source: DBObjectExtractor) = PaymentLimit(
      value = source.field("v").value[BigDecimal],
      currency = source.field("c").value[CurrencyId]
    )
  }
}
