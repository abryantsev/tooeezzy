package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import java.util.Date
import com.tooe.core.db.mongo.domain.CacheAdminSession
import com.tooe.core.domain.{CompanyId, SessionToken, AdminRoleId, AdminUserId}

@WritingConverter
class CacheAdminSessionWriteConverter extends Converter[CacheAdminSession, DBObject] with CacheAdminSessionConverter {

  def convert(source: CacheAdminSession): DBObject = cacheAdminSessionConverter.serialize(source)
}

@ReadingConverter
class CacheAdminSessionReadConverter extends Converter[DBObject, CacheAdminSession] with CacheAdminSessionConverter {

  def convert(source: DBObject): CacheAdminSession = cacheAdminSessionConverter.deserialize(source)
}

trait CacheAdminSessionConverter {

  import DBObjectConverters._

  implicit val cacheAdminSessionConverter = new DBObjectConverter[CacheAdminSession] {

    def serializeObj(obj: CacheAdminSession) = DBObjectBuilder()
      .id.value(obj.id)
      .field("t").value(obj.time)
      .field("uid").value(obj.adminUserId)
      .field("r").value(obj.role)
      .field("cids").optSeq(obj.companiesOpt)

    def deserializeObj(source: DBObjectExtractor) = CacheAdminSession(
      id = source.id.value[SessionToken],
      time = source.field("t").value[Date],
      adminUserId = source.field("uid").value[AdminUserId],
      role = source.field("r").value[AdminRoleId],
      companies = source.field("cids").seqOpt[CompanyId] getOrElse Nil
    )
  }
}