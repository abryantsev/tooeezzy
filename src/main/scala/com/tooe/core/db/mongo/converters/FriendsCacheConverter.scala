package com.tooe.core.db.mongo.converters

import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import org.springframework.core.convert.converter.Converter
import com.tooe.core.db.mongo.domain.FriendsCache
import com.mongodb.DBObject
import com.tooe.core.domain.{FriendsCacheId, UserId}
import java.util.Date


@WritingConverter
class FriendsCacheWriteConverter extends Converter[FriendsCache, DBObject] with FriendsCacheConverter {

  def convert(source: FriendsCache): DBObject = friendsCacheConverter.serialize(source)


}

@ReadingConverter
class FriendsCacheReadConverter extends Converter[DBObject, FriendsCache] with FriendsCacheConverter {
  def convert(source: DBObject): FriendsCache = friendsCacheConverter.deserialize(source)
}

trait FriendsCacheConverter {

  import DBObjectConverters._

  implicit val friendsCacheConverter = new DBObjectConverter[FriendsCache] {

    def serializeObj(obj: FriendsCache) = DBObjectBuilder()
      .id.value(obj.id)
      .field("uid").value(obj.userId)
      .field("gid").value(obj.friendGroupId)
      .field("t").value(obj.creationTime)
      .field("fs").value(obj.friends)

    def deserializeObj(source: DBObjectExtractor) = FriendsCache(
      id = source.id.value[FriendsCacheId],
      userId = source.field("uid").value[UserId],
      friendGroupId = source.field("gid").opt[String],
      creationTime = source.field("t").value[Date],
      friends = source.field("fs").seq[UserId]
    )

  }

}
