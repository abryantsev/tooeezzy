package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.db.mongo.domain.ProductRef
import com.tooe.core.db.mongo.domain.Wish
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.ProductId
import com.tooe.core.domain.WishId

@WritingConverter
class WishWriteConverter extends Converter[Wish, DBObject] with WishConverter {

  def convert(source: Wish) = WishConverter.serialize(source)

}

@ReadingConverter
class WishReadConverter extends Converter[DBObject, Wish] with WishConverter {

  def convert(source: DBObject) = WishConverter.deserialize(source)

}

trait WishConverter {

  import DBObjectConverters._

  implicit val ProductRefConverter = new DBObjectConverter[ProductRef] {
    def serializeObj(obj: ProductRef) = DBObjectBuilder()
      .field("lid").value(obj.locationId)
      .field("pid").value(obj.productId)

    def deserializeObj(source: DBObjectExtractor) = ProductRef(
      locationId = source.field("lid").value[LocationId],
      productId = source.field("pid").value[ProductId]
    )
  }

  implicit val WishConverter = new DBObjectConverter[Wish] {
    def serializeObj(obj: Wish) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("p").value(obj.product)
      .field("r").value(obj.reason)
      .field("rd").value(obj.reasonDate)
      .field("t").value(obj.creationDate)
      .field("ft").value(obj.fulfillmentDate)
      .field("lc").value(obj.likesCount)
      .field("ls").value(obj.usersWhoSetLikes)
      .field("lfs").value(obj.lifecycleStatus)

    def deserializeObj(source: DBObjectExtractor) = Wish(
      id = source.id.value[WishId],
      userId = source.field("uid").value[UserId],
      product = source.field("p").value[ProductRef],
      reason = source.field("r").opt[String],
      reasonDate = source.field("rd").opt[Date],
      creationDate = source.field("t").value[Date],
      fulfillmentDate = source.field("ft").opt[Date],
      likesCount = source.field("lc").value[Int](0),
      usersWhoSetLikes = source.field("ls").seq[UserId],
      lifecycleStatus = source.field("lfs").opt[String]
    )
  }
}
