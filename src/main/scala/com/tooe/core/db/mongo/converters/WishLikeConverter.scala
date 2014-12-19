package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.WritingConverter
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.{UserId, WishId, WishLikeId}
import java.util.Date

@WritingConverter
class WishLikeWriteConverter extends Converter[WishLike, DBObject] with WishLikeConverter {

  def convert(source: WishLike) = wishLikeConverter.serialize(source)

}

@ReadingConverter
class WishLikeReadConverter extends Converter[DBObject, WishLike] with WishLikeConverter {

  def convert(source: DBObject) = wishLikeConverter.deserialize(source)

}

trait WishLikeConverter {

  import DBObjectConverters._

  implicit val wishLikeConverter = new DBObjectConverter[WishLike] {
    def serializeObj(obj: WishLike) = DBObjectBuilder()
      .id.value(obj.id)
      .field("wid").value(obj.wishId)
      .field("t").value(obj.created)
      .field("uid").value(obj.userId)

    def deserializeObj(source: DBObjectExtractor) = WishLike(
      id = source.id.value[WishLikeId],
      wishId = source.field("wid").value[WishId],
      created = source.field("t").value[Date],
      userId = source.field("uid").value[UserId]
    )
  }
}