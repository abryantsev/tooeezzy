package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.AdminRole
import com.tooe.core.domain.AdminRoleId
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject


@WritingConverter
class AdminRoleWriteConverter extends Converter[AdminRole, DBObject] with AdminRoleConverter {

  def convert(source: AdminRole) = adminRoleConverter.serialize(source)

}

@ReadingConverter
class AdminRoleReadConverter extends Converter[DBObject, AdminRole] with AdminRoleConverter {

  def convert(source: DBObject) = adminRoleConverter.deserialize(source)

}

trait AdminRoleConverter {

  import DBObjectConverters._

  implicit val adminRoleConverter = new DBObjectConverter[AdminRole] {
    def serializeObj(obj: AdminRole) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = AdminRole(
      id = source.id.value[AdminRoleId],
      name = source.field("n").objectMap[String]
    )
  }

}
