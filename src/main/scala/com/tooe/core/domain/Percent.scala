package com.tooe.core.domain

case class Percent(value: Int) {
  require(value >= 0 && value <= 100)
}

object Percent {

  implicit def fromInt(value: Int): Percent = Percent(value)
}