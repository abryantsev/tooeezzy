package com.tooe.core

import java.util.{TimeZone, Date}
import java.text.SimpleDateFormat
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain.ObjectMap

package object service {

  implicit class DateMongoReprHelper(val value: Date) extends AnyVal {
    def mongoRepr: String = {
      def formatter(formatStr: String): SimpleDateFormat = {
        val timeZone = TimeZone.getTimeZone("GMT")
        val result = new SimpleDateFormat(formatStr)
        result.setTimeZone(timeZone)
        result
      }
      val formattedDate = formatter("yyyy-MM-dd").format(value)
      val formattedTime = formatter("HH:mm:ss.SSS").format(value)
      s"""{ "$$date" : "${formattedDate}T${formattedTime}Z"}"""
    }

  }
  
  implicit class ObjectIdMongoReprHelper(val value: ObjectId) extends AnyVal {
    def mongoRepr: String = s"""{ "$$oid" : "${value.toString}" }"""
  }
  implicit class ObjectMapMongoReprHelper(val value: ObjectMap[String]) extends AnyVal {
    def mongoRepr: String = value.map.map{ case (k, v) => k + ":" + v }.mkString("{", ",", "}")
  }
}