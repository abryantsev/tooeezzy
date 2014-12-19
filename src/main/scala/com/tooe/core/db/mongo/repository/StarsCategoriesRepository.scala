package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.StarCategory

trait StarsCategoriesRepository extends MongoRepository[StarCategory, String] {

}
