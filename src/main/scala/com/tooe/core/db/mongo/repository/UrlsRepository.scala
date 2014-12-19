package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.Urls

trait UrlsRepository extends MongoRepository[Urls, ObjectId] {

}
