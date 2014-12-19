package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date

@Document(collection = "sale")
case class Sale
(
  id: ObjectId = new ObjectId,
  @Field("p") product: ProductShort,
  @Field("pc") productCategories: Seq[String] = Nil,
  @Field("lo") location: LocationShort,
  @Field("st") startDate: Date,
  @Field("et") endDate: Date,
  @Field("pd") percentDiscount: Option[Int] = None,
  @Field("ad") absoluteDiscount: Option[Int] = None,
  @Field("sm") saleMedia: ArrayList[SaleMedia] = Nil,
  @Field("ia") isActiveFlag: Option[Boolean] = None
  )

case class ProductShort
(
  @Field("pid") productId: ObjectId,
  @Field("n") name: ObjectMap[String]
  )

case class LocationShort
(
  @Field("lid") locationId: ObjectId,
  @Field("rid") regionId: ObjectId
  )

case class SaleMedia(@Field("u") url: String)
