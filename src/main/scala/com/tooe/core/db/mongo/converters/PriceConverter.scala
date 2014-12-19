package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.{CurrencyId, Price}

trait PriceConverter {

  import DBObjectConverters._

  implicit val priceConverter = new DBObjectConverter[Price] {

    def serializeObj(obj: Price) = DBObjectBuilder()
      .field("v").value(obj.value)
      .field("c").value(obj.currency)

    def deserializeObj(source: DBObjectExtractor) = Price(
      value = source.field("v").value[BigDecimal],
      currency = source.field("c").value[CurrencyId]
    )
  }

}