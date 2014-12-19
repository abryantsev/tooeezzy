package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "infomessage")
case class InfoMessage
(
  id: String,
  @Field("m") message: ObjectMap[String],
  @Field("c") errorCode: Int 
    )