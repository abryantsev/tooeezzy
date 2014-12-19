package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity}
import java.util.Date
import com.tooe.core.domain._
import com.tooe.core.domain.LocationCategoryId
import com.tooe.core.domain.RegionId

@Document(collection = "promotion")
case class Promotion (
  id: PromotionId = PromotionId(),
  name: ObjectMap[String] = ObjectMap.empty,
  description: ObjectMap[String] = ObjectMap.empty,
  additionalInfo: Option[ObjectMap[String]] = None,
  media: Seq[MediaUrl] = Nil,
  dates: promotion.Dates,
  price: Option[ObjectMap[String]] = None,
  location: promotion.Location,
  visitorsCount: Int = 0
)

package promotion {
  case class Location
  (
    location: LocationId,
    name: ObjectMap[String],
    region: RegionId,
    categories: Seq[LocationCategoryId]
    )

  case class Dates
  (
    start: Date,
    end: Option[Date] = None,
    time: Option[Date] = None, //TODO should have special type for time only
    period: PromotionPeriod = PromotionPeriod.Default
    )

}