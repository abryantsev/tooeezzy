package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.WritingConverter
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.{PromotionId, PromotionVisitorId, PromotionStatus, UserId}
import java.util.Date

@WritingConverter
class PromotionVisitorWriteConverter extends Converter[PromotionVisitor, DBObject] with PromotionVisitorConverter {

  def convert(source: PromotionVisitor): DBObject = promotionVisitorConverter.serialize(source)

}

@ReadingConverter
class PromotionVisitorReadConverter extends Converter[DBObject, PromotionVisitor] with PromotionVisitorConverter {

  def convert(source: DBObject): PromotionVisitor = promotionVisitorConverter.deserialize(source)

}

trait PromotionVisitorConverter {

  import DBObjectConverters._

  implicit val promotionVisitorConverter = new DBObjectConverter[PromotionVisitor] {
    def serializeObj(obj: PromotionVisitor) = DBObjectBuilder()
      .id.value(obj.id)
      .field("pid").value(obj.promotion)
      .field("uid").value(obj.visitor)
      .field("s").value(obj.status)
      .field("t").value(obj.time)

    def deserializeObj(source: DBObjectExtractor) = PromotionVisitor(
      id = source.id.value[PromotionVisitorId],
      promotion = source.field("pid").value[PromotionId],
      visitor = source.field("uid").value[UserId],
      status = source.field("s").value[PromotionStatus],
      time = source.field("t").value[Date]
    )
  }
}