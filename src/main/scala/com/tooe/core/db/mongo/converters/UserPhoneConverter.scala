package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{WritingConverter, ReadingConverter}
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import java.util.Date

@WritingConverter
class UserPhoneWriteConverter extends Converter[UserPhone, DBObject] with UserPhoneConverter {
  def convert(obj: UserPhone) = UserPhoneConverter.serialize(obj)
}

@ReadingConverter
class UserPhoneReadConverter extends Converter[DBObject, UserPhone] with UserPhoneConverter {
  def convert(source: DBObject) = UserPhoneConverter.deserialize(source)
}

trait UserPhoneConverter
  extends PhoneConverter
{
  import DBObjectConverters._

  implicit val UserPhoneConverter = new DBObjectConverter[UserPhone] {
    def serializeObj(obj: UserPhone) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("p").value(obj.phone)

    def deserializeObj(source: DBObjectExtractor) = UserPhone(
      id = source.id.value[UserPhoneId],
      userId = source.field("uid").value[UserId],
      phone = source.field("p").value[Phone]
    )
  }
}