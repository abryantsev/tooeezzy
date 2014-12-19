package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.UsersGroup
import org.bson.types.ObjectId

trait UsersGroupRepository  extends MongoRepository[UsersGroup, ObjectId]{

}
