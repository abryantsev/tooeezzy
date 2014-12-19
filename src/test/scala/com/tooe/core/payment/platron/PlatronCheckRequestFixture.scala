package com.tooe.core.payment.platron

import com.tooe.core.payment.plantron._
import com.tooe.core.payment.plantron.PlatronCheckRequest

object PlatronCheckRequestFixture {

  def request = PlatronCheckRequest(
    orderId = BigInt(102348),
    salt = "8765",
    paymentId = "765432",
    paymentSystem = "WEBMONEYR",
    amount = BigDecimal(100),
    currency = "RUR",
    psCurrency = "RUR",
    psAmount = BigDecimal(101.12),
    psFullAmount = BigDecimal(104.98)
  )

  def asPlatronParams(request: PlatronCheckRequest): PlatronParams = {
    import request._
    Seq( //TODO should all the parameters get from request
      'pg_order_id -> orderId.toString,
      'pg_salt -> salt,
      'pg_payment_id -> paymentId,
      'pg_payment_system -> paymentSystem,
      'pg_amount -> "100.00",
      'pg_currency -> currency,
      'pg_ps_currency -> psCurrency,
      'pg_ps_amount -> "101.12",
      'pg_ps_full_amount -> "104.98"
    )
  }
}