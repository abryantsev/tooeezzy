package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.EventGroupId

@Document(collection = "eventgroup")
case class EventGroup(id: EventGroupId, name: ObjectMap[String])
