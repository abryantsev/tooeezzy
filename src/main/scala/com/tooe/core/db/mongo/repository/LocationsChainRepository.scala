package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.LocationsChain
import org.bson.types.ObjectId

trait LocationsChainRepository extends MongoRepository[LocationsChain, ObjectId]