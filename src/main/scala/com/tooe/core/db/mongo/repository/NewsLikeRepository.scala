package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.NewsLike
import org.bson.types.ObjectId

trait NewsLikeRepository extends MongoRepository [NewsLike, ObjectId]