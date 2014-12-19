package com.tooe.core.db.mongo.repository

import org.springframework.data.mongodb.repository.MongoRepository
import com.tooe.core.db.mongo.domain.NewsComment
import org.bson.types.ObjectId

trait NewsCommentRepository extends MongoRepository [NewsComment, ObjectId]
