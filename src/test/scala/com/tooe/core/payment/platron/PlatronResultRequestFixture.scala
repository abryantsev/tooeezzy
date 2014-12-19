package com.tooe.core.payment.platron

import com.tooe.core.payment.plantron._
import java.text.SimpleDateFormat
import com.tooe.core.payment.plantron.PlatronResultRequest

class PlatronResultRequestFixture(fullPrice: Option[BigDecimal] = Some(BigDecimal("9.99"))) {
  import com.tooe.core.util.SomeWrapper._

  def request = PlatronResultRequest(
    orderId = BigInt(102348),
    salt = "35a1d",
    paymentId = "5747770",
    amount = BigDecimal("9.9900"),
    currency = "RUR",
    netAmount = BigDecimal("9.99"),
    psAmount = BigDecimal("9.99"),
    psFullAmount = fullPrice,
    psCurrency = "RUR",
    paymentSystem = "TEST",
    result = true,
    paymentDate = {
      val df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss")
      df.parse("2013-03-15 15:05:34")
    },
    canReject = false,
    userPhone = "79058388488",
    failureCode = "some code",
    failureDescription = "some description"
  )

  val ConfirmationRequest = request.copy(result = true)
  val RejectionRequest = request.copy(result = false)

  def asPlatronParams(request: PlatronResultRequest = request): PlatronParams = {
    import request._
    Seq( //TODO should all the parameters get from request
      'pg_order_id -> orderId.toString,
      'pg_salt -> salt,
      'pg_payment_id -> paymentId.toString,
      'pg_amount -> "9.9900",
      'pg_currency -> currency,
      'pg_net_amount -> "9.99",
      'pg_ps_amount -> "9.99",
      'pg_ps_full_amount -> request.psFullAmount.get.toString(),
      'pg_ps_currency -> psCurrency.get,
      'pg_payment_system -> paymentSystem,
      'pg_result -> (if (request.result) "1" else "0"),
      'pg_payment_date -> "2013-03-15 15:05:34",
      'pg_can_reject -> "0",
      'pg_user_phone -> userPhone,
      'pg_failure_code -> failureCode.get,
      'pg_failure_description -> failureDescription.get
    )
  }
}