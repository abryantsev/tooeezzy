package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UserId, LocationNewsId, LocationNewsLikeId}
import java.util.Date

@Document(collection = "location_news_likes")
case class LocationNewsLike(id: LocationNewsLikeId = LocationNewsLikeId(),
                             locationNewsId: LocationNewsId,
                             time: Date = new Date,
                             userId: UserId)