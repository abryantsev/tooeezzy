package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.AdminUserEvent
import org.bson.types.ObjectId

trait AdminUserEventRepository extends MongoRepository[AdminUserEvent, ObjectId]  {

}
