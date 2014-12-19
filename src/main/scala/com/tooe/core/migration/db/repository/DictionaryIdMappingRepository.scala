package com.tooe.core.migration.db.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.migration.db.domain.DictionaryIdMapping
import org.bson.types.ObjectId

trait DictionaryIdMappingRepository extends MongoRepository[DictionaryIdMapping, ObjectId] {

}