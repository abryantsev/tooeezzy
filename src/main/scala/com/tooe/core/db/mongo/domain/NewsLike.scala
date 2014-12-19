package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UserId, NewsId}
import org.bson.types.ObjectId
import java.util.Date

@Document(collection = "news_like")
case class NewsLike
(id: NewsLikeId,
 newsId: NewsId,
 time: Date,
 userId: UserId)

object NewsLike {

  def apply(userId: UserId, newsId: NewsId): NewsLike =
    NewsLike(NewsLikeId(new ObjectId()),newsId, new Date(),userId)
}

case class NewsLikeId(id: ObjectId)