package com.tooe.core.util

import scala.reflect.ClassTag
import com.tooe.core.db.mongo.util.JacksonModuleScalaSupport

object JsonDeserializer extends JsonDeserializer

trait JsonDeserializer {

  def deserializeMap(source: String) = deserialize[Map[String, Any]](source)

  def deserialize[T](source: String)(implicit m: ClassTag[T]) = JacksonModuleScalaSupport.deserialize[T](source)
}