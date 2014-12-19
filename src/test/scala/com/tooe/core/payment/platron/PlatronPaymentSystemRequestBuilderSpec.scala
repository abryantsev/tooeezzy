package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.util.SomeWrapper
import com.tooe.core.payment.plantron._

class PlatronPaymentSystemRequestBuilderSpec extends SpecificationWithJUnit {

  import SomeWrapper._
  val platronRequest = PlatronPaymentSystemRequest(
    merchantId = "23435",
    amount = BigDecimal("9999.99"),
    currency = "RUB"
  )
  val expectedRequest: PlatronParams = {
    import platronRequest._
    Seq(
      'pg_merchant_id -> merchantId,
      'pg_amount -> "9999.99",
      'pg_salt -> salt,
      'pg_currency -> currency.get,
      'pg_testing_mode -> "1"
    )
  }

  "PlatronPaymentSystemRequestBuilder" should {
    import PlatronPaymentSystemRequestBuilder._
    "generate proper request params" >> {
      build(platronRequest).toMap === expectedRequest.toMap
    }
  }
}