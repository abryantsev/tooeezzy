package com.tooe.core.db.mongo.converters

import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.domain.{LifecycleStatusId, AdminRoleId, CompanyId, AdminUserId}
import java.util.Date
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject

@WritingConverter
class AdminUserWriteConverter extends Converter[AdminUser, DBObject] with AdminUserConverter {

  def convert(source: AdminUser): DBObject = adminUserConverter.serialize(source)

}

@ReadingConverter
class AdminUserReadConverter extends Converter[DBObject, AdminUser] with AdminUserConverter {

  def convert(source: DBObject): AdminUser = adminUserConverter.deserialize(source)
}

trait AdminUserConverter {

  import DBObjectConverters._

  implicit val adminUserConverter = new DBObjectConverter[AdminUser] {

    def serializeObj(obj: AdminUser) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)
      .field("ln").value(obj.lastName)
      .field("ns").value(obj.names)
      .field("rt").value(obj.registrationDate)
      .field("r").value(obj.role)
      .field("cid").value(obj.companyId)
      .field("d").value(obj.description)
      .field("lfs").value(obj.lifecycleStatus)

    def deserializeObj(source: DBObjectExtractor) = AdminUser(
      id = source.id.value[AdminUserId],
      name = source.field("n").value[String],
      lastName = source.field("ln").value[String],
      registrationDate = source.field("rt").value[Date],
      role = source.field("r").value[AdminRoleId],
      companyId = source.field("cid").opt[CompanyId],
      description = source.field("d").opt[String],
      lifecycleStatus = source.field("lfs").opt[LifecycleStatusId]
    )

  }

}
