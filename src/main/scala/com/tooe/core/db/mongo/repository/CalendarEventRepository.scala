package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.CalendarEvent
import org.bson.types.ObjectId

trait CalendarEventRepository extends MongoRepository[CalendarEvent, ObjectId]