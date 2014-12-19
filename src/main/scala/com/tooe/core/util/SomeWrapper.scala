package com.tooe.core.util

object SomeWrapper {
  implicit def toSome[T](v: T): Some[T] = Some(v)
}
