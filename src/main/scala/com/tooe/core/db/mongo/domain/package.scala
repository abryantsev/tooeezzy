package com.tooe.core.db.mongo

import org.springframework.data.mongodb.core.mapping.{Field => MongoField}
import annotation.meta.field

package object domain {
  type Field = MongoField @field

  type ArrayList[T] = java.util.ArrayList[T]

  object ArrayList {
    def empty[T] = new ArrayList[T]
  }

  import scala.collection.JavaConverters._
  implicit def fromSeq[T](seq: Seq[T]): ArrayList[T] = new ArrayList(seq.asJavaCollection)
  implicit def fromArrayList[T](arrayList: ArrayList[T]): Seq[T] = arrayList.asScala
}
