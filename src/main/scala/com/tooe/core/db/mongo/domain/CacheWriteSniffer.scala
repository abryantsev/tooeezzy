package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import com.tooe.core.domain.{WriteSnifferCacheId, UserId}

@Document(collection = "cache_writesniffer")
case class CacheWriteSniffer
(
  id: WriteSnifferCacheId = WriteSnifferCacheId(),
  userId: UserId,
  createdAt: Date
  )