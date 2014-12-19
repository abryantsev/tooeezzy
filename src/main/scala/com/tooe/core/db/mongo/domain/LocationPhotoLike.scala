package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UserId, LocationPhotoId, LocationPhotoLikeId}
import java.util.Date

@Document( collection = "location_photo_like" )
case class LocationPhotoLike(id: LocationPhotoLikeId = LocationPhotoLikeId(),
  locationPhotoId: LocationPhotoId,
  time: Date = new Date,
  userId: UserId)