package com.tooe.core.payment.platron

import org.specs2.mutable.SpecificationWithJUnit
import com.tooe.core.util.{HashHelper, SomeWrapper}
import com.tooe.core.payment.plantron._

class PlatronRequestBuilderSpec extends SpecificationWithJUnit {

  import SomeWrapper._
  val platronRequest = PlatronRequest(
    merchantId = "23435",
    orderId = BigInt(98789),
    amount = BigDecimal("9999.99"),
    description = "Burger",
    currency = "RUB",
    userPhone = "+7 (934) 005-34-56",
    userContactEmail = "somebody@slfjk.sdfklh",
    successUrl = HashHelper.uuid,
    successUrlMethod = HashHelper.uuid,
    failureUrl = HashHelper.uuid,
    failureUrlMethod = HashHelper.uuid,
    paymentSystem = "CASH",
    additionalParams = Seq('pg_someparam -> "some value")
  )
  val expectedRequest: PlatronParams = {
    import platronRequest._
    Seq(
      'pg_merchant_id -> merchantId,
      'pg_order_id -> "98789",
      'pg_amount -> "9999.99",
      'pg_description -> description,
      'pg_salt -> salt,
      'pg_currency -> currency.get,
      'pg_testing_mode -> "1",
      'pg_user_phone -> userPhone.get,
      'pg_user_contact_email -> userContactEmail.get,
      'pg_success_url -> successUrl.get,
      'pg_success_url_method -> successUrlMethod.get,
      'pg_failure_url -> failureUrl.get,
      'pg_failure_url_method -> failureUrlMethod.get,
      'pg_payment_system -> paymentSystem.get
    ) ++ platronRequest.additionalParams
  }

  "PlatronRequestBuilder" should {
    import PlatronRequestBuilder._
    "generate proper request params" >> {
      build(platronRequest).toMap === expectedRequest.toMap
    }
  }
}