package com.tooe.core.db.mongo.converters

import com.tooe.core.domain._
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.core.domain.UrlsId
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.db.mongo.domain.Urls
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class UrlsWriteConverter extends Converter[Urls, DBObject] with UrlsConverter {
  def convert(obj: Urls) = urlsConverter.serialize(obj)
}

@ReadingConverter
class UrlsReadConverter extends Converter[DBObject, Urls] with UrlsConverter {
  def convert(source: DBObject) = urlsConverter.deserialize(source)
}


trait UrlsConverter {
  import DBObjectConverters._

  implicit val urlsConverter = new DBObjectConverter[Urls] {

    def serializeObj(obj: Urls) = DBObjectBuilder()
      .id.value(obj.id)
      .field("e").value(obj.entityType)
      .field("eid").value(obj.entityId)
      .field("t").value(obj.time)
      .field("uri").value(obj.mediaId)
      .field("ef").value(obj.entityField)
      .field("ut").value(obj.urlType)
      .field("rt").value(obj.readTime)


    def deserializeObj(source: DBObjectExtractor) = Urls(
      id = source.id.value[UrlsId],
      entityType = source.field("e").value[EntityType],
      entityId = source.field("eid").value[ObjectId],
      time = source.field("t").value[Date],
      mediaId = source.field("uri").value[MediaObjectId],
      entityField = source.field("ef").opt[String],
      urlType = source.field("ut").opt[UrlType],
      readTime = source.field("rt").opt[Date]
    )
  }
}
