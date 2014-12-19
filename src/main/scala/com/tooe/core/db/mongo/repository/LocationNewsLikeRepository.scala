package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.LocationNewsLike
import org.bson.types.ObjectId

trait LocationNewsLikeRepository extends MongoRepository[LocationNewsLike, ObjectId] {

}
