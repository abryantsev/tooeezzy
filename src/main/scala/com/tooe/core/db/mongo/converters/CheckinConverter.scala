package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain.Checkin
import com.tooe.core.db.mongo.domain.UserInfoCheckin
import com.tooe.core.db.mongo.domain.LocationInfoCheckin
import com.tooe.core.domain.{UserId, CheckinId}
import java.util.Date

@WritingConverter
class CheckinWriteConverter extends Converter[Checkin, DBObject] with CheckinConverter {

  def convert(source: Checkin): DBObject = сheckinConverter.serialize(source)

}

@ReadingConverter
class CheckinReadConverter extends Converter[DBObject, Checkin] with CheckinConverter {

  def convert(source: DBObject): Checkin = сheckinConverter.deserialize(source)

}

trait CheckinConverter extends LocationInfoCheckinConverter with UserInfoCheckinConverter {

  import DBObjectConverters._

  implicit val сheckinConverter = new DBObjectConverter[Checkin] {

    def serializeObj(obj: Checkin) = DBObjectBuilder()
      .id.value(obj.id.id)
      .field("t").value(obj.creationTime)
      .field("u").value(obj.user)
      .field("lo").value(obj.location)
      .field("fs").value(obj.friends)

    def deserializeObj(source: DBObjectExtractor) =  Checkin(
      id = source.id.value[CheckinId],
      creationTime = source.field("t").value[Date],
      user = source.field("u").value[UserInfoCheckin],
      location = source.field("lo").value[LocationInfoCheckin],
      friends = source.field("fs").seq[UserId]
    )

  }
}