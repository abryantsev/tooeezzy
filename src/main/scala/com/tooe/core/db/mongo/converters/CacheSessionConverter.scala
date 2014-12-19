package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.CacheSession
import com.tooe.core.domain.{UserId, SessionToken}
import java.util.Date

@WritingConverter
class CacheSessionWriteConverter extends Converter[CacheSession, DBObject] with CacheSessionConverter {

  def convert(source: CacheSession): DBObject = cacheSessionConverter.serialize(source)

}

@ReadingConverter
class CacheSessionReadConverter extends Converter[DBObject, CacheSession] with CacheSessionConverter {

  def convert(source: DBObject): CacheSession = cacheSessionConverter.deserialize(source)
}

trait CacheSessionConverter {

  import DBObjectConverters._

  implicit val cacheSessionConverter = new DBObjectConverter[CacheSession] {

    def serializeObj(obj: CacheSession) = DBObjectBuilder()
      .id.value(obj.id.hash)
      .field("t").value(obj.createdAt)
      .field("uid").value(obj.userId)

    def deserializeObj(source: DBObjectExtractor) =  CacheSession(
      id = source.id.value[SessionToken],
      createdAt = source.field("t").value[Date],
      userId = source.field("uid").value[UserId]
    )

  }

}