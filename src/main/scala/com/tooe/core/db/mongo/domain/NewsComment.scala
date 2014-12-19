package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UserId, NewsId, NewsCommentId}
import java.util.Date
import org.bson.types.ObjectId

@Document(collection = "news_comment")
case class NewsComment
(
  id: NewsCommentId = NewsCommentId(id = new ObjectId),
  parentId: Option[NewsCommentId],
  newsId: NewsId,
  creationDate: Date,
  message: String,
  authorId: UserId
)
