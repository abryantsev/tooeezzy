package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.FavoriteStats
import org.bson.types.ObjectId

trait FavoriteStatsRepository extends MongoRepository[FavoriteStats, ObjectId]{

}
