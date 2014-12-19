package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.EventTypeId

@Document( collection = "eventtype" )
case class EventType(id: EventTypeId,
                     eventGroups : Seq[String],
                     name : ObjectMap[String],
                     message : ObjectMap[String],
                     userEventMessage : ObjectMap[String])

