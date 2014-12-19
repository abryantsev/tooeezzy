package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.CacheUserOnline
import com.tooe.core.domain.{OnlineStatusId, UserId}
import java.util.Date

@WritingConverter
class CacheUserOnlineWriteConverter extends Converter[CacheUserOnline, DBObject] with CacheUserOnlineConverter {

  def convert(source: CacheUserOnline): DBObject = cacheUserOnlineConverter.serialize(source)
}

@ReadingConverter
class CacheUserOnlineReadConverter extends Converter[DBObject, CacheUserOnline] with CacheUserOnlineConverter {

  def convert(source: DBObject): CacheUserOnline = cacheUserOnlineConverter.deserialize(source)
}

trait CacheUserOnlineConverter {

  import DBObjectConverters._

  implicit val cacheUserOnlineConverter = new DBObjectConverter[CacheUserOnline] {

    def serializeObj(obj: CacheUserOnline) = DBObjectBuilder()
      .id.value(obj.id)
      .field("t").value(obj.createdAt)
      .field("os").value(obj.onlineStatusId)
      .field("fs").value(obj.friends)

    def deserializeObj(source: DBObjectExtractor) =  CacheUserOnline(
      id = source.id.value[UserId],
      createdAt = source.field("t").value[Date],
      onlineStatusId = source.field("os").value[OnlineStatusId],
      friends = source.field("fs").seq[UserId]
    )
  }
}