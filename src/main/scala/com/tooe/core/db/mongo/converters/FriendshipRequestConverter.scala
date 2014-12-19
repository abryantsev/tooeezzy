package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.tooe.core.db.mongo.domain.FriendshipRequest
import com.mongodb.DBObject
import com.tooe.core.domain.{FriendshipRequestId, UserId}
import java.util.Date

@WritingConverter
class FriendshipRequestWriteConverter extends Converter[FriendshipRequest, DBObject] with FriendshipRequestConverter {
  def convert(source: FriendshipRequest): DBObject = FriendshipRequestConverter.serialize(source)
}

@ReadingConverter
class FriendshipRequestReadConverter extends Converter[DBObject, FriendshipRequest] with FriendshipRequestConverter {
  def convert(source: DBObject): FriendshipRequest = FriendshipRequestConverter.deserialize(source)
}

trait FriendshipRequestConverter {

  import DBObjectConverters._

  implicit val FriendshipRequestConverter = new DBObjectConverter[FriendshipRequest] {

    def serializeObj(obj: FriendshipRequest) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("aid").value(obj.actorId)
      .field("t").value(obj.createdAt)

    def deserializeObj(source: DBObjectExtractor) = FriendshipRequest(
      id = source.id.value[FriendshipRequestId],
      userId = source.field("uid").value[UserId],
      actorId = source.field("aid").value[UserId],
      createdAt = source.field("t").value[Date]
    )
  }
}