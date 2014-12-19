package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.bson.types.ObjectId
import java.util.Date

@Document(collection = "present_status")
case class PresentStatus(
                          id: String,
                          @Field("n") name: ObjectMap[String] = ObjectMap.empty
                          )


