package com.tooe.core.domain

case class PeriodId(id: String)

object PeriodId {
  val day = PeriodId("day")
  val week = PeriodId("week")
  val month = PeriodId("month")
}