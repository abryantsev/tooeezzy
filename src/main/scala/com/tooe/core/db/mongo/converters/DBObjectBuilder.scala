package com.tooe.core.db.mongo.converters

import com.mongodb.BasicDBObject
import com.tooe.core.db.mongo.domain
import domain._
import scala.collection.JavaConverters._
import java.util

case class DBObjectBuilder(dbObject: BasicDBObject = new BasicDBObject) {


  def add(key: String, value: Any) = DBObjectBuilder(dbObject.append(key, value))

  def id = field("_id")

  def field(key: String) = new {

    def value[T : DBSimpleConverter](v: T): DBObjectBuilder = {
      val converter = implicitly[DBSimpleConverter[T]]
      add(key, converter.serialize(v))
    }
    
    def value[T : DBSimpleConverter](v: Option[T]): DBObjectBuilder = {
      v map (value(_)) getOrElse DBObjectBuilder.this
    }

    def optSeq[T : DBSimpleConverter](v: Option[Seq[T]]): DBObjectBuilder = {
      v map (value(_)) getOrElse DBObjectBuilder.this
    }

    def value[T : DBSimpleConverter](v: Seq[T]): DBObjectBuilder = {
      val converter = implicitly[DBSimpleConverter[T]]
      val serializedSeq = v map converter.serialize
      add(key, new util.ArrayList(serializedSeq.asJavaCollection))
    }

    def value[T : DBSimpleConverter](source: ObjectMap[T]): DBObjectBuilder = {
      val converter = implicitly[DBSimpleConverter[T]]
      val serializedMap = new BasicDBObject
      for ((k, v) <- source) {
        serializedMap.put(k, converter deserialize v)
      }
      add(key, serializedMap)
    }

    def optObjectMap[T : DBSimpleConverter](source: Option[ObjectMap[T]]): DBObjectBuilder = source map (value(_)) getOrElse DBObjectBuilder.this

  }
}