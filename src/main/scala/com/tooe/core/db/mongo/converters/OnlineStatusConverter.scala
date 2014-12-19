package com.tooe.core.db.mongo.converters


import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._

@WritingConverter
class OnlineStatusWriteConverter extends Converter[OnlineStatus, DBObject] with OnlineStatusConverter {

  def convert(source: OnlineStatus): DBObject = onlineStatusConverter.serialize(source)

}

@ReadingConverter
class OnlineStatusReadConverter extends Converter[DBObject, OnlineStatus] with OnlineStatusConverter {

  def convert(source: DBObject): OnlineStatus = onlineStatusConverter.deserialize(source)

}

trait OnlineStatusConverter {

  import DBObjectConverters._

  implicit val onlineStatusConverter = new DBObjectConverter[OnlineStatus] {

    def serializeObj(obj: OnlineStatus) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = OnlineStatus(
      id = source.id.value[OnlineStatusId],
      name = source.field("n").objectMap[String]
    )

  }
}
