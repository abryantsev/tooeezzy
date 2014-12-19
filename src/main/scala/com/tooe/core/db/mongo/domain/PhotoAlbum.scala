package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import com.tooe.core.domain.{MediaObject, UserId, PhotoAlbumId}
import java.util.Date

@Document(collection = "photoalbum")
case class PhotoAlbum
(
  id: PhotoAlbumId = PhotoAlbumId(),
  userId: UserId,
  name: String,
  description: Option[String] = None,
  count: Int = 0,
  frontPhotoUrl: MediaObject,
  allowedView: Seq[String] = Nil,
  allowedComment: Seq[String] = Nil,
  createdTime: Date = new Date,
  default: Option[Boolean] = None
  )