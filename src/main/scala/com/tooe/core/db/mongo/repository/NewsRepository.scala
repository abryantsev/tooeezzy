package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.News
import org.bson.types.ObjectId

trait NewsRepository extends MongoRepository[News, ObjectId]{

}
