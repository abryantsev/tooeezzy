package com.tooe.core.domain

sealed trait Unsetable[+A] {
  import Unsetable._

  def get: A

  @inline final def map[B](f: A => B): Unsetable[B] = this match {
    case Unset         => Unset
    case Skip          => Skip
    case Update(value) => Update(f(value))
  }

  @inline final def flatMap[B](f: A => Unsetable[B]): Unsetable[B] = this match {
    case Unset         => Unset
    case Skip          => Skip
    case Update(value) => f(value)
  }

  @inline final def toOption: Option[A] = this match {
    case Unset => None
    case Skip => None
    case Update(value) => Some(value)
  }

  @inline final def toSeq: Seq[A] = toOption.toSeq
}

object Unsetable {
  case class Update[T](value: T) extends Unsetable[T] {
    def get = value
  }
  case object Unset extends Unsetable[Nothing] {
    def get = throw new NoSuchElementException("Unsetable.Unset.get")
  }
  case object Skip extends Unsetable[Nothing] {
    def get = throw new NoSuchElementException("Unsetable.Skip.get")
  }
}
