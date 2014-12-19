package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.promotion
import com.tooe.core.domain.{LocationCategoryId, RegionId, LocationId}

trait PromotionLocationConverter {

  import DBObjectConverters._

  implicit val promotionLocationConverter = new DBObjectConverter[promotion.Location] {
    def serializeObj(obj: promotion.Location) = DBObjectBuilder()
      .field("lid").value(obj.location)
      .field("n").value(obj.name)
      .field("rid").value(obj.region)
      .field("lc").value(obj.categories)

    def deserializeObj(source: DBObjectExtractor) = promotion.Location(
      location = source.field("lid").value[LocationId],
      name = source.field("n").objectMap[String],
      region = source.field("rid").value[RegionId],
      categories = source.field("lc").seq[LocationCategoryId]
    )
  }

}
