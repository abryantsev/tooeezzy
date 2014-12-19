package com.tooe.core.db.mongo.converters

import org.bson.BSONObject
import com.mongodb.DBObject

trait DBSimpleConverter[T] {
  def serialize(value: T): Any
  def deserialize(source: Any): T
}

trait DBObjectConverter[T] extends DBSimpleConverter[T] {
  protected def serializeObj(obj: T): DBObjectBuilder
  protected def deserializeObj(source: DBObjectExtractor): T

  def serialize(obj: T): DBObject = serializeObj(obj).dbObject
  def deserialize(source: Any): T = deserializeObj(new DBObjectExtractor(source.asInstanceOf[BSONObject]))
}