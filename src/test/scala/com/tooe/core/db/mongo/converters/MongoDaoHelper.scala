package com.tooe.core.db.mongo.converters

import com.mongodb.{DBCollection, DBObject}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.MongoClient
import org.bson.types.ObjectId
import com.tooe.core.infrastructure.AppContextTestHelper

case class MongoProperties(host: String, port: Int, database: String)

class MongoDaoHelper(collectionName: String)
{
  lazy val mongoProps = AppContextTestHelper.lookupBean[MongoProperties]

  lazy val mongodb = MongoClient(mongoProps.host, mongoProps.port)(mongoProps.database)

  def collection: DBCollection = mongodb.getCollection(collectionName)

  def findOne(id: String): DBObject = findOneRetry(id)
  def findOne(id: ObjectId): DBObject = findOneRetry(id)

  def findOneRetry[T](id: T): DBObject = {
    (1 to 5) foreach { _ =>
      val result = collection.findOne(MongoDBObject("_id" -> id))
      if (result != null) return result
      else null
      Thread.sleep(10L)
    }
    throw new IllegalStateException("Not found")
  }

  def count(): Long = collection.count()
}