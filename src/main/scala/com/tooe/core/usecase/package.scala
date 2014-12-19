package com.tooe.core

import exceptions.NotFoundException
import org.bson.types.ObjectId
import scala.reflect.ClassTag

package object usecase {

  implicit class OptionWrapper[T](val option: Option[T]) extends AnyVal {

    def getOrFailure(exception: => Exception): Any = option getOrElse akka.actor.Status.Failure(exception)

    def getOrNotFound(id: Any, entity: String): Any =
      getOrFailure(NotFoundException(s"${entity}(id=$id) does not exist"))

    def getOrNotFoundException(message: String): T =
      option.getOrElse( throw NotFoundException(message))

    def getOrNotFound(id: ObjectId, entity: String): Any = getOrNotFound(id.toString, entity)
  }

  implicit class SeqToMapIdHelper[ Entity  : ClassTag](val entities: Seq[Entity]) {
    def toMapId[B](keyField: Entity => B) = entities.map(e => (keyField(e), e)).toMap
  }
}