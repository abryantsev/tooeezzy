package com.tooe.core.db.mongo.converters

import com.tooe.core.domain.{Percent, Discount}
import java.util.Date

trait DiscountConverter {

  import DBObjectConverters._

  implicit val discountConverter = new DBObjectConverter[Discount] {

    def serializeObj(obj: Discount) = DBObjectBuilder()
      .field("pd").value(obj.percent)
      .field("st").value(obj.startDate)
      .field("et").value(obj.endDate)

    def deserializeObj(source: DBObjectExtractor) = Discount(
      percent = source.field("pd").value[Percent],
      startDate = source.field("st").opt[Date],
      endDate = source.field("et").opt[Date]
    )
  }

}