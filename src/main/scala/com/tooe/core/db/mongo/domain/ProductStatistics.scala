package com.tooe.core.db.mongo.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date


@Document( collection = "product_statistics")
case class ProductStatistics
(
   @Id @Field("_id") id: ObjectId,
   @Field("pid") productId: ObjectId,
   @Field("ct") creationDate: Date,
   @Field("at") activationDate: Date,
   @Field("it") inactivationDate: Date,
   @Field("vc") visitorsCount: Int
)