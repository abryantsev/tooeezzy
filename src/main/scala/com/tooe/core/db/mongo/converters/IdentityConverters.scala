package com.tooe.core.db.mongo.converters

import java.util.Date
import org.bson.types.ObjectId
import com.mongodb.DBObject

trait IdentityConverters {

  class IdentityConverter[T] extends DBSimpleConverter[T] {
    def serialize(obj: T) = obj
    def deserialize(source: Any) = source.asInstanceOf[T]
  }
  
  implicit val stringConverter = new IdentityConverter[String]
  implicit val dateConverter = new IdentityConverter[Date]
  implicit val booleanConverter = new IdentityConverter[Boolean]
  implicit val objectIdConverter = new IdentityConverter[ObjectId]
  implicit val dbObjectConverter = new IdentityConverter[DBObject]
}

object IdentityConverters extends IdentityConverters