package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.tooe.core.db.mongo.domain.StarSubscription
import com.mongodb.DBObject
import com.tooe.core.domain.{StarSubscriptionId, UserId}


@WritingConverter
class StarSubscriptionWriteConvert extends Converter[StarSubscription, DBObject] with StarSubscriptionConvert {
  def convert(obj: StarSubscription) = StarSubscriptionConverter.serialize(obj)
}

@ReadingConverter
class StarSubscriptionReadConvert extends Converter[DBObject, StarSubscription] with StarSubscriptionConvert {
  def convert(source: DBObject) = StarSubscriptionConverter.deserialize(source)
}

trait StarSubscriptionConvert
{
  import DBObjectConverters._

  implicit val StarSubscriptionConverter = new DBObjectConverter[StarSubscription] {
    def serializeObj(obj: StarSubscription) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("sid").value(obj.starId)

    def deserializeObj(source: DBObjectExtractor) = StarSubscription(
      id = source.id.value[StarSubscriptionId],
      userId = source.field("uid").value[UserId],
      starId = source.field("sid").value[UserId]
    )
  }
}
