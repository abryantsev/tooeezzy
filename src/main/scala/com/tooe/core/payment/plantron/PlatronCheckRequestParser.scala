package com.tooe.core.payment.plantron

import com.tooe.core.db.mysql.domain.Payment

object PlatronCheckRequestParser {

  def parse(request: PlatronParams): PlatronCheckRequest = {
    val rp = request.toMap
    PlatronCheckRequest(
      orderId = BigInt(rp('pg_order_id)),
      salt = rp('pg_salt),
      paymentId = rp('pg_payment_id),
      paymentSystem = rp('pg_payment_system),
      amount = BigDecimal(rp('pg_amount)),
      currency = rp('pg_currency),
      psCurrency = rp('pg_ps_currency),
      psAmount = BigDecimal(rp('pg_ps_amount)),
      psFullAmount = BigDecimal(rp('pg_ps_full_amount))
    )
  }
}

case class PlatronCheckRequest
(
  orderId: Payment.OrderId,
  salt: String,
  paymentId: String,
  paymentSystem: String,
  amount: BigDecimal,
  currency: String,
  psCurrency: String,
  psAmount: BigDecimal,
  psFullAmount: BigDecimal
  )