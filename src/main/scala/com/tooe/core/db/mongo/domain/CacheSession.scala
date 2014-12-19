package com.tooe.core.db.mongo.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.domain.UserId
import com.tooe.core.domain.SessionToken

@Document(collection = "cache_sessions")
case class CacheSession
(
  id: SessionToken,
  createdAt: Date,
  userId: UserId
)