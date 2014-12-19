package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.LocationNews
import org.bson.types.ObjectId

trait LocationNewsRepository extends MongoRepository[LocationNews, ObjectId] {

}
