package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date
import com.tooe.core.domain.{PhotoId, PhotoLikeId, UserId}

@Document( collection = "photo_like" )
case class PhotoLike(id: PhotoLikeId = PhotoLikeId(),
    @Field("pid") photoId: PhotoId,
    @Field("t") time: Date,
    @Field("uid") userId: UserId) {
}
