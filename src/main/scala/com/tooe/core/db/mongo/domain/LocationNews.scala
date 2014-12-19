package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.{LocationsChainId, UserId, LocationId, LocationNewsId}
import java.util.Date
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "location_news")
case class LocationNews(id: LocationNewsId = LocationNewsId(),
                        locationId: LocationId,
                        content: ObjectMap[String] = ObjectMap.empty,
                        commentsEnabled: Option[Boolean],
                        createdTime: Date = new Date,
                        likesCount: Int = 0,
                        lastLikes: Seq[UserId] = Nil,
                        locationsChainId: Option[LocationsChainId])
