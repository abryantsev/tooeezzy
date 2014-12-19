package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{LocationPhotoCommentId, UserId, LocationPhotoId, LocationPhotoLikeId}
import java.util.Date

@Document( collection = "location_photo_comment" )
case class LocationPhotoComment(id: LocationPhotoCommentId = LocationPhotoCommentId(),
    locationPhotoId: LocationPhotoId,
    time: Date = new Date,
    userId: UserId,
    message: String)