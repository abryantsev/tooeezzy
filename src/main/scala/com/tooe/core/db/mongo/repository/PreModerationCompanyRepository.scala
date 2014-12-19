package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.PreModerationCompany
import org.bson.types.ObjectId

trait PreModerationCompanyRepository extends MongoRepository[PreModerationCompany, ObjectId] {
}