package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.LocationSubscription
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.UserId

@WritingConverter
class LocationSubscriptionWriteConverter extends Converter[LocationSubscription, DBObject] with LocationSubscriptionConverter {
  def convert(source: LocationSubscription) = subscriptionConverter.serialize(source)
}

@ReadingConverter
class LocationSubscriptionReadConverter extends Converter[DBObject, LocationSubscription] with LocationSubscriptionConverter {

  def convert(source: DBObject) = subscriptionConverter.deserialize(source)

}

trait LocationSubscriptionConverter {

  import DBObjectConverters._

  implicit val subscriptionConverter = new DBObjectConverter[LocationSubscription] {
    def serializeObj(obj: LocationSubscription) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("lid").value(obj.locationId)

    def deserializeObj(source: DBObjectExtractor) = LocationSubscription(
      id = source.id.value[LocationSubscriptionId],
      userId = source.field("uid").value[UserId],
      locationId = source.field("lid").value[LocationId]
    )
  }
}