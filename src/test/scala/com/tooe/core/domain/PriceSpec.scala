package com.tooe.core.domain

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.matcher.MustMatchers

class PriceSpec extends SpecificationWithJUnit with MustMatchers {

  "Price" should {
    "with discount" >> {
      Price(BigDecimal(100), CurrencyId.RUR).withDiscount(100) === Price(BigDecimal(0), CurrencyId.RUR)
      Price(BigDecimal(100), CurrencyId.RUR).withDiscount(0) === Price(BigDecimal(100), CurrencyId.RUR)
      Price(BigDecimal(100), CurrencyId.RUR).withDiscount(80) === Price(BigDecimal(20), CurrencyId.RUR)
      Price(BigDecimal(1), CurrencyId.RUR).withDiscount(20) === Price(BigDecimal(0.80), CurrencyId.RUR)
    }
  }
}