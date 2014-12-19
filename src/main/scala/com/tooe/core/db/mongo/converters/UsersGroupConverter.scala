package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.tooe.core.db.mongo.domain.UsersGroup
import com.mongodb.DBObject
import com.tooe.core.domain.UsersGroupId

@WritingConverter
class UsersGroupWriteConverter extends Converter[UsersGroup, DBObject] with UsersGroupConverter {

  def convert(source: UsersGroup): DBObject = usersGroupConverter.serialize(source)
}

@ReadingConverter
class UsersGroupReadConverter extends Converter[DBObject, UsersGroup] with UsersGroupConverter {
  def convert(source: DBObject): UsersGroup = usersGroupConverter.deserialize(source)
}

trait UsersGroupConverter {

  import DBObjectConverters._

  implicit val usersGroupConverter = new DBObjectConverter[UsersGroup] {
    def serializeObj(obj: UsersGroup) = DBObjectBuilder()
      .id.value(obj.id)
      .field("n").value(obj.name)

    def deserializeObj(source: DBObjectExtractor) = UsersGroup(
      id = source.id.value[UsersGroupId],
      name = source.field("n").objectMap[String]
    )
  }
}
