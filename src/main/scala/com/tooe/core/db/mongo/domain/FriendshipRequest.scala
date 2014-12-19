package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{FriendshipRequestId, UserId}
import java.util.Date

@Document( collection = "friendshiprequest" )
case class FriendshipRequest
(
  id: FriendshipRequestId = FriendshipRequestId(),
  userId: UserId,
  actorId: UserId,
  createdAt: Date
  )