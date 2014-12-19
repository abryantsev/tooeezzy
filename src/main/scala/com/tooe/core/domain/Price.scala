package com.tooe.core.domain

case class Price(value: BigDecimal, currency: CurrencyId) {
  def withDiscount(percent: Int) = Price(value - value / 100 * percent, currency)
}