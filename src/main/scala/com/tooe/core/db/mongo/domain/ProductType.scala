package com.tooe.core.db.mongo.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document( collection = "product_type")
case class ProductType
(
  @Id @Field("_id") id: String,
  @Field("n") name: ObjectMap[String])