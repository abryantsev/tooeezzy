package com.tooe.core.migration.db.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.migration.db.domain.IdMapping
import org.bson.types.ObjectId

trait IdMappingRepository extends MongoRepository[IdMapping, ObjectId] {

}