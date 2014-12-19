package com.tooe.core.payment.plantron

import com.tooe.core.util.HashHelper
import com.tooe.core.util.FormatHelper._

object PlatronPaymentSystemRequestBuilder extends PlatronMessageBuilder {

  def build(request: PlatronPaymentSystemRequest): PlatronParams = {
    import request._
    Seq(
      'pg_merchant_id -> merchantId,
      'pg_amount -> formatBigDecimal(amount),
      'pg_salt -> salt
    ) ++
      optParam('pg_currency, currency) ++
      (if (testingMode) Some('pg_testing_mode -> "1") else None)
  }
}
case class PlatronPaymentSystemRequest
(
  merchantId: String,
  amount: BigDecimal,
  currency: Option[String] = None,
  testingMode: Boolean = true,
  salt: String = HashHelper.uuid
  )