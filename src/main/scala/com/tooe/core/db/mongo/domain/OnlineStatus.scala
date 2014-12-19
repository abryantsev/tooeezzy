package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.OnlineStatusId

@Document(collection = "online_status")
case class OnlineStatus(id: OnlineStatusId,
                        name: ObjectMap[String] = ObjectMap.empty)