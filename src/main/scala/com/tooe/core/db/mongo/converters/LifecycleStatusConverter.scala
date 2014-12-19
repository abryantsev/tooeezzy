package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.LifecycleStatus
import com.tooe.core.domain.LifecycleStatusId
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class LifecycleStatusWriteConverter extends Converter[LifecycleStatus, DBObject] with LifecycleStatusConverter {

  def convert(source: LifecycleStatus) = lifecycleStatusConverter.serialize(source)

}

@ReadingConverter
class LifecycleStatusReadConverter extends Converter[DBObject, LifecycleStatus] with LifecycleStatusConverter {

  def convert(source: DBObject) = lifecycleStatusConverter.deserialize(source)

}

trait LifecycleStatusConverter {

  import DBObjectConverters._

  implicit val lifecycleStatusConverter = new DBObjectConverter[LifecycleStatus] {
    def serializeObj(obj: LifecycleStatus) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = LifecycleStatus(
      id = source.id.value[LifecycleStatusId],
      name = source.field("n").objectMap[String]
    )
  }

}
