package com.tooe.core.payment.plantron

import java.util.Date
import annotation.switch
import java.text.SimpleDateFormat

object PlatronResultRequestParser {

  def parse(request: PlatronParams): PlatronResultRequest = {
    val rp = request.toMap
    val rpOpt = request.toMap.get _
    PlatronResultRequest(
      orderId = BigInt(rp('pg_order_id)),
      salt = rp('pg_salt),
      paymentId = rp('pg_payment_id),
      amount = BigDecimal(rp('pg_amount)),
      currency = rp('pg_currency),
      netAmount = BigDecimal(rp('pg_net_amount)),
      psAmount = rpOpt('pg_ps_amount) map BigDecimal.apply,
      psFullAmount = rpOpt('pg_ps_full_amount) map BigDecimal.apply,
      psCurrency = rpOpt('pg_ps_currency),
      paymentSystem = rp('pg_payment_system),
      result = asBoolean(rp('pg_result)),
      paymentDate = asDate(rp('pg_payment_date)),
      canReject = asBoolean(rp('pg_can_reject)),
      userPhone = rp('pg_user_phone),
      failureCode = rpOpt('pg_failure_code),
      failureDescription = rpOpt('pg_failure_description)
    )
  }

  private def asBoolean(in: String): Boolean = (in: @switch) match {
    case "1" => true
    case "0" => false
  }

  private def asDate(in: String): Date = {
    val df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss")
    df.parse(in)
  }
}

case class PlatronResultRequest
(
  orderId: BigInt,
  salt: String,
  paymentId: String,
  amount: BigDecimal,
  currency: String,
  netAmount: BigDecimal,
  psAmount: Option[BigDecimal],
  psFullAmount: Option[BigDecimal],
  psCurrency: Option[String],
  paymentSystem: String,
  result: Boolean,
  paymentDate: Date,
  canReject: Boolean,
  userPhone: String,
  failureCode: Option[String],
  failureDescription: Option[String]
)