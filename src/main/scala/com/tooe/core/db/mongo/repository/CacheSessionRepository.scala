package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.CacheSession
import com.tooe.core.domain.SessionToken

trait CacheSessionRepository extends MongoRepository[CacheSession, String]