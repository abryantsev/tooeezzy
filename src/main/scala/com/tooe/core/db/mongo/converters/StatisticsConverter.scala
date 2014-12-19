package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.Statistics
import com.tooe.core.domain.{LocationCategoryId, StarCategoryId}

trait StatisticsConverter {
  import DBObjectConverters._

  implicit val StatisticsConverter = new DBObjectConverter[Statistics] {
    def serializeObj(obj: Statistics) = DBObjectBuilder()
      .field("l").value(obj.locationsCount)
      .field("p").value(obj.promotionsCount)
      .field("s").value(obj.salesCount)
      .field("u").value(obj.usersCount)
      .field("pr").value(obj.presentsCount)
      .field("f").value(obj.favoritesCount)
      .field("sc").value(obj.starCategories)
      .field("lc").value(obj.locationCategories)

    def deserializeObj(source: DBObjectExtractor) = Statistics(
      locationsCount = source.field("l").value[Int](0),
      promotionsCount = source.field("p").value[Int](0),
      salesCount = source.field("s").value[Int](0),
      usersCount = source.field("u").value[Int](0),
      presentsCount = source.field("pr").value[Int](0),
      favoritesCount = source.field("f").value[Int](0),
      starCategories = source.field("sc").seq[StarCategoryId],
      locationCategories = source.field("lc").seq[LocationCategoryId]
    )
  }
}
