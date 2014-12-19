package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.domain.{OnlineStatusId, UserId}

@Document(collection = "cache_useronline")
case class CacheUserOnline
(
  id: UserId,
  createdAt: Date,
  onlineStatusId: OnlineStatusId,
  friends: Seq[UserId]
)