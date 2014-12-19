package com.tooe.core.db.mongo.domain

import com.tooe.core.domain.ModerationStatusId
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "moderation_status")
case class ModerationStatus(id: ModerationStatusId, name: ObjectMap[String], description: ObjectMap[String])