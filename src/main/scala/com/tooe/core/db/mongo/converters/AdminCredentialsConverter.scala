package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.AdminCredentials
import com.tooe.core.domain.{AdminCredentialsId, AdminUserId}
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class AdminCredentialsWriteConverter extends Converter[AdminCredentials, DBObject] with AdminCredentialsConverter {

  def convert(source: AdminCredentials): DBObject = adminCredentialsConverter.serialize(source)

}

@ReadingConverter
class AdminCredentialsReadConverter extends Converter[DBObject, AdminCredentials] with AdminCredentialsConverter {

  def convert(source: DBObject): AdminCredentials = adminCredentialsConverter.deserialize(source)
}

trait AdminCredentialsConverter {

  import DBObjectConverters._

  implicit val adminCredentialsConverter = new DBObjectConverter[AdminCredentials] {

    def serializeObj(obj: AdminCredentials) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.adminUserId)
      .field("un").value(obj.userName.toLowerCase.trim) //no
      .field("pwd").value(obj.password)
      .field("lpwd").value(obj.legacyPassword)

    def deserializeObj(source: DBObjectExtractor) = AdminCredentials(
      id = source.id.value[AdminCredentialsId],
      adminUserId = source.field("uid").value[AdminUserId],
      userName = source.field("un").value[String],
      password = source.field("pwd").value[String],
      legacyPassword = source.field("lpwd").opt[String]
    )

  }

}
