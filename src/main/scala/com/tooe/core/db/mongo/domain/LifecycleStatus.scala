package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.{LifecycleStatusId, MaritalStatusId}

@Document(collection = "lifecycle_status")
case class LifecycleStatus(id: LifecycleStatusId,
                         name: ObjectMap[String] = ObjectMap.empty)