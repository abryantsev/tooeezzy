package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{FriendsCacheId, UserId}
import org.bson.types.ObjectId
import java.util.Date

@Document( collection = "cache_friends" )
case class FriendsCache
(
  id: FriendsCacheId = FriendsCacheId(),
  userId: UserId = UserId(),
  friendGroupId: Option[String] = None,
  creationTime: Date = new Date(),
  friends: Seq[UserId] = Nil
)


