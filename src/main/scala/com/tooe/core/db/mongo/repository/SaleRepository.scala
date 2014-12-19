package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.Sale

trait SaleRepository  extends MongoRepository[Sale, ObjectId] {
}
