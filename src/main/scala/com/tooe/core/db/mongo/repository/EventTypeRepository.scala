package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.EventType

trait EventTypeRepository extends MongoRepository[EventType, String]
