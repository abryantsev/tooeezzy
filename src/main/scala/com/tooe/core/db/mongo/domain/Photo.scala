package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{MediaObject, UserId, PhotoAlbumId, PhotoId}
import java.util.Date

@Document( collection = "photo" )
case class Photo(id: PhotoId = PhotoId(),
   photoAlbumId: PhotoAlbumId,
   createdAt: Date = new Date,
   userId: UserId,
   name: Option[String],
   fileUrl: MediaObject,
   likesCount: Int = 0,
   usersLikesIds: Seq[UserId] = Nil,
   commentsCount: Int = 0,
   usersCommentsIds:Seq[UserId] = Nil
) {

  def userIds = usersLikesIds ++ usersCommentsIds

}