package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.AdminUserEvent
import com.tooe.core.domain.{AdminUserId, AdminUserEventId}
import java.util.Date
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class AdminUserEventWriteConverter extends Converter[AdminUserEvent, DBObject] with AdminUserEventConverter {

  def convert(source: AdminUserEvent): DBObject = adminUserEventConverter.serialize(source)

}

@ReadingConverter
class AdminUserEventReadConverter extends Converter[DBObject, AdminUserEvent] with AdminUserEventConverter {

  def convert(source: DBObject): AdminUserEvent = adminUserEventConverter.deserialize(source)
}


trait AdminUserEventConverter {

  import DBObjectConverters._

  implicit val adminUserEventConverter = new DBObjectConverter[AdminUserEvent] {

    def serializeObj(obj: AdminUserEvent) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.adminUserId)
      .field("t").value(obj.createdTime)
      .field("m").value(obj.message)

    def deserializeObj(source: DBObjectExtractor) = AdminUserEvent(
      id = source.id.value[AdminUserEventId],
      adminUserId = source.field("uid").value[AdminUserId],
      createdTime = source.field("t").value[Date],
      message = source.field("m").value[String]
    )

  }

}
