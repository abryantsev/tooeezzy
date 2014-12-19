package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.core.domain.{PhotoCommentId, PhotoId, UserId}

@Document(collection = "photo_comment")
case class PhotoComment(id: ObjectId = new ObjectId,
  @Field("pid")  photoObjectId:ObjectId,
  @Field("t") time: Date = new Date,
  @Field("m") message: String,
  @Field("uid") authorObjId:ObjectId
) {

  def photoCommentId = PhotoCommentId(id)

  def authorId = UserId(authorObjId)

  def photoId = PhotoId(photoObjectId)

}
