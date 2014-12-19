package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.{UserStatisticsNew, UserStatistics}

trait UserStatisticsConverter {
  import DBObjectConverters._

  implicit val UserStatisticNewConverter = new DBObjectConverter[UserStatisticsNew] {
    def serializeObj(obj: UserStatisticsNew) = DBObjectBuilder()
      .field("prc").value(obj.presentsCount)
      .field("ec").value(obj.eventsCount)
    def deserializeObj(source: DBObjectExtractor) = UserStatisticsNew(
      presentsCount = source.field("prc").value[Int](0),
      eventsCount = source.field("ec").value[Int](0)
    )
  }

  implicit val UserStatisticsConverter = new DBObjectConverter[UserStatistics] {
    def serializeObj(obj: UserStatistics) = DBObjectBuilder()
      .field("prc").value(obj.presentsCount)
      .field("cc").value(obj.certificatesCount)
      .field("spc").value(obj.sentPresentsCount)
      .field("scc").value(obj.sentCertificatesCount)
      .field("fc").value(obj.friendsCount)
      .field("frc").value(obj.friendsRequestCount)
      .field("wc").value(obj.wishesCount)
      .field("fwc").value(obj.fulfilledWishesCount)
      .field("flc").value(obj.favoriteLocationsCount)
      .field("ssc").value(obj.starSubscriptionsCount)
      .field("lsc").value(obj.locationSubscriptionsCount)
      .field("pc").value(obj.photoAlbumsCount)
      .field("ec").value(obj.eventsCount)
      .field("new").value(obj.newStatistic)

    def deserializeObj(source: DBObjectExtractor) = UserStatistics(
      presentsCount = source.field("prc").value[Int](0),
      certificatesCount = source.field("cc").value[Int](0),
      sentPresentsCount = source.field("spc").value[Int](0),
      sentCertificatesCount = source.field("scc").value[Int](0),
      friendsCount = source.field("fc").value[Int](0),
      friendsRequestCount = source.field("frc").value[Int](0),
      wishesCount = source.field("wc").value[Int](0),
      fulfilledWishesCount = source.field("fwc").value[Int](0),
      favoriteLocationsCount = source.field("flc").value[Int](0),
      starSubscriptionsCount = source.field("ssc").value[Int](0),
      locationSubscriptionsCount = source.field("lsc").value[Int](0),
      photoAlbumsCount = source.field("pc").value[Int](0),
      eventsCount = source.field("ec").value[Int](0),
      newStatistic = source.field("new").value[UserStatisticsNew]
    )
  }
}
