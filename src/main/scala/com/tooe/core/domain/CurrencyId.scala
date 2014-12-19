package com.tooe.core.domain

case class CurrencyId(id: String)

object CurrencyId {
  val RUR = CurrencyId("RUR")

  def code(id: CurrencyId): Int = id match {
    case RUR => 810
  }
}