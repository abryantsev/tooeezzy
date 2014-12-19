package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date

@Document( collection = "saleevent")
case class SaleEvent
(
  id: ObjectId,
  @Field("n") name: ObjectMap[String],
  @Field("d") description: ObjectMap[String],
  @Field("lo") location: LocationShort,
  @Field("st") startDate: Date,
  @Field("et") endDate: Date,
  @Field("at") actionEvent: Option[Date],
  @Field("p") periodForEvent: String, // “day” | “week” | “month
  @Field("ne") percentDiscount: Option[Int],
  @Field("ad") absoluteDiscount: Option[Int],
  @Field("pd") saleMedia: Seq[EvenMedia],
  @Field("ia") isActiveFlag: Option[Boolean]
)

case class EvenMedia(@Field("u") url: String)
