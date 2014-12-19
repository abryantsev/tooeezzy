package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.LocationStatistics
import org.bson.types.ObjectId


trait LocationStatisticsRepository extends MongoRepository[LocationStatistics, ObjectId]
