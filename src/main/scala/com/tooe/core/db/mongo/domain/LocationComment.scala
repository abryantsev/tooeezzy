package com.tooe.core.db.mongo.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date

@Document( collection = "location_comment" )
case class LocationComment
(
   @Id @Field("_id") id: ObjectId,
   @Field("lid") locationOwner: ObjectId,
   @Field("t") creationDate: Date,
   @Field("m") message: String,
   @Field("uid") userAuthorId: ObjectId,
   @Field("lc") likesCount: Option[Int],
   @Field("ls") usersWhoSetLikesId: Option[Seq[ObjectId]],
   @Field("cc") commentsCount: Option[Int],
   @Field("cs") commentsChildren: Option[Seq[ObjectId]],
   @Field("cu") commentsAuthorsId: Option[Seq[ObjectId]]
)