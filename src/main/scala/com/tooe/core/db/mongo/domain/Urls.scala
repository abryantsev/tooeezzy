package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{UrlType, MediaObjectId, EntityType, UrlsId}
import org.bson.types.ObjectId
import java.util.Date
import org.joda.time.DateTime

@Document(collection = "urls")
case class Urls(id: UrlsId = UrlsId(),
                 entityType: EntityType,
                 entityId: ObjectId,
                 time: Date = DateTime.now().plusMinutes(10).toDate,
                 mediaId: MediaObjectId,
                 entityField: Option[String] = None, //TODO will use later, now only declaration
                 urlType: Option[UrlType] = Some(UrlType.s3),
                 readTime: Option[Date] = None)
