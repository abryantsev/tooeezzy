package com.tooe.core.util

import com.mongodb.BasicDBObject
import org.springframework.data.mongodb.core.query.Query

object ProjectionHelper {

  import scala.reflect.runtime.universe._

  def generateProjectionDBObject[T](presenceFields: Set[String])(implicit ttag: TypeTag[T]) = {
    presenceFields.foldLeft(new BasicDBObject())((res, next) => res.append(next, 1))
  }


  implicit def queryProjectionHelper(query: Query) = new {

    def extendProjection(presenceFields: Seq[String]): Query = {
      presenceFields.foreach(query.fields().include)
      query
    }

    def extendProjection(presenceFields: Set[String]): Query = extendProjection(presenceFields.toSeq)
  }
}