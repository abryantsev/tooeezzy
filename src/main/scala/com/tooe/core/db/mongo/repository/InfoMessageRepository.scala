package com.tooe.core.db.mongo.repository

import com.tooe.core.db.mongo.domain.InfoMessage
import org.springframework.data.mongodb.repository.MongoRepository

trait InfoMessageRepository extends MongoRepository[InfoMessage, String]