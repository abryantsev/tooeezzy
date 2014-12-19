package com.tooe.core.db.mongo.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.{ReadingConverter, WritingConverter}
import com.tooe.core.db.mongo.domain.ObjectMap
import com.mongodb.{DBObject, BasicDBObject}
import scala.collection.JavaConverters._

@WritingConverter
class ObjectMapWriteConverter extends Converter[ObjectMap[_], DBObject] {

  override def convert(source: ObjectMap[_]) = {
    val result = new BasicDBObject
    for ((key, value) <- source) {
      result.put(key, value)
    }
    result
  }
}

@ReadingConverter
class ObjectMapReadConverter extends Converter[DBObject, ObjectMap[_]] {

  override def convert(source: DBObject) = {
    val keys = source.keySet().asScala
    val keyValues = keys.map{ k =>
      k -> source.get(k)
    }
    ObjectMap(keyValues.toMap)
  }
}