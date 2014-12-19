package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.core.domain.{WishId, WishLikeId, UserId}

@Document(collection = "wish_likes")
case class WishLike
(
  id: WishLikeId = WishLikeId(new ObjectId),
  wishId: WishId,
  created: Date,
  userId: UserId
  )