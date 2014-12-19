package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.promotion
import java.util.Date
import com.tooe.core.domain.PromotionPeriod

trait PromotionDatesConverter extends MediaUrlConverter {

  import DBObjectConverters._

  implicit val promotionDatesConverter = new DBObjectConverter[promotion.Dates] {
    def serializeObj(obj: promotion.Dates) = DBObjectBuilder()
      .field("st").value(obj.start)
      .field("et").value(obj.end)
      .field("at").value(obj.time)
      .field("p").value(obj.period)

    def deserializeObj(source: DBObjectExtractor) = promotion.Dates(
      start = source.field("st").value[Date],
      end = source.field("et").opt[Date],
      time = source.field("at").opt[Date],
      period = source.field("p").value[PromotionPeriod]
    )
  }

}