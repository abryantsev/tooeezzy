package com.tooe.core.db.mongo.converters

import com.mongodb.DBObject
import com.tooe.core.db.mongo.domain
import domain._
import java.util.ArrayList
import scala.collection.JavaConverters._
import org.bson.BSONObject

class DBObjectExtractor(source: BSONObject) {
  
  import DBObjectConverters._
  
  def id = field(IdField)
  
  def field(field: String) = new {
    def value[T <: AnyRef : DBSimpleConverter]: T = opt[T] getOrElse null.asInstanceOf[T]

    def value[T <: NotNull : DBSimpleConverter](default: T): T = opt[T] getOrElse default

    def opt[T : DBSimpleConverter]: Option[T] = Option(source.get(field)) map deserialize[T]

    def seq[T: DBSimpleConverter]: Seq[T] = seqOpt[T] getOrElse Seq.empty[T]

    def seqOpt[T: DBSimpleConverter]: Option[Seq[T]] = Option(source.get(field)) map {
      _.asInstanceOf[ArrayList[Any]].asScala map deserialize[T]
    }

    def objectMap[T: DBSimpleConverter]: ObjectMap[T] = optObjectMap[T] getOrElse ObjectMap.empty[T]

    def optObjectMap[T: DBSimpleConverter]: Option[ObjectMap[T]] = Option(source.get(field)) map { source =>
      val dbObject = source.asInstanceOf[DBObject]
      val keys = dbObject.keySet().asScala
      val keyValues = keys map { k => k -> deserialize[T](dbObject.get(k)) }
      ObjectMap(keyValues.toMap)
    }

    def deserialize[T: DBSimpleConverter](obj: Any): T = implicitly[DBSimpleConverter[T]] deserialize obj
  }
  

}