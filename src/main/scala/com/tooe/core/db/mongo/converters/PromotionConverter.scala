package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.WritingConverter
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.domain.Promotion
import com.tooe.core.domain.{MediaUrl, PromotionId}

@WritingConverter
class PromotionWriteConverter extends Converter[Promotion, DBObject] with PromotionConverter {

  def convert(source: Promotion): DBObject = promotionConverter.serialize(source)

}

@ReadingConverter
class PromotionReadConverter extends Converter[DBObject, Promotion] with PromotionConverter{

  def convert(source: DBObject): Promotion = promotionConverter.deserialize(source)

}

trait PromotionConverter extends MediaUrlConverter with PromotionDatesConverter with PromotionLocationConverter {

  import DBObjectConverters._

  implicit val promotionConverter = new DBObjectConverter[Promotion] {
    def serializeObj(obj: Promotion) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("d").value(obj.description)
      .field("i").optObjectMap(obj.additionalInfo)
      .field("pm").value(obj.media)
      .field("ds").value(obj.dates)
      .field("pc").optObjectMap(obj.price)
      .field("l").value(obj.location)
      .field("vc").value(obj.visitorsCount)

    def deserializeObj(source: DBObjectExtractor) = Promotion(
      id = source.id.value[PromotionId],
      name = source.field("n").objectMap[String],
      description = source.field("d").objectMap[String],
      additionalInfo = source.field("i").optObjectMap[String],
      media = source.field("pm").seq[MediaUrl],
      dates = source.field("ds").value[promotion.Dates],
      price = source.field("pc").optObjectMap[String],
      location = source.field("l").value[promotion.Location],
      visitorsCount = source.field("vc").value[Int](0)
    )
  }

}