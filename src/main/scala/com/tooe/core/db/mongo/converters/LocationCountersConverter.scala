package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LocationCounters

trait LocationCountersConverter {

  import DBObjectConverters._

  implicit val locationCountersConverter = new DBObjectConverter[LocationCounters] {
    def serializeObj(obj: LocationCounters) = DBObjectBuilder()
      .field("prc").value(obj.presentsCount)
      .field("pac").value(obj.photoalbumsCount)
      .field("rc").value(obj.reviewsCount)
      .field("sc").value(obj.subscribersCount)
      .field("fc").value(obj.favoritePlaceCount)
      .field("cc").value(obj.countOfCheckins)
      .field("pc").value(obj.productsCount)

    def deserializeObj(source: DBObjectExtractor) = LocationCounters(
      presentsCount = source.field("prc").value[Int](0),
      photoalbumsCount = source.field("pac").value[Int](0),
      reviewsCount = source.field("rc").value[Int](0),
      subscribersCount = source.field("sc").value[Int](0),
      favoritePlaceCount = source.field("fc").value[Int](0),
      countOfCheckins = source.field("cc").value[Int](0),
      productsCount = source.field("pc").value[Int](0)
    )
  }

}
