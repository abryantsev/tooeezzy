package com.tooe.core.payment.plantron

import com.tooe.core.util.{HashHelper, FormatHelper}
import com.tooe.core.db.mysql.domain.Payment

object PlatronRequestBuilder extends PlatronMessageBuilder {

  import FormatHelper._

  def build(request: PlatronRequest): PlatronParams = {
    import request._
    Seq(
      'pg_merchant_id -> merchantId,
      'pg_order_id -> formatBigInt(orderId),
      'pg_amount -> formatBigDecimal(amount),
      'pg_description -> description,
      'pg_salt -> salt
    ) ++
    optParam('pg_currency, currency) ++
    (if (testingMode) Some('pg_testing_mode -> "1") else None) ++
    optParam('pg_user_phone, userPhone) ++
    optParam('pg_user_contact_email, userContactEmail) ++
    optParam('pg_request_method, requestMethod) ++
    optParam('pg_check_url, checkUrl) ++
    optParam('pg_result_url, resultUrl) ++
    optParam('pg_success_url, successUrl) ++
    optParam('pg_success_url_method, successUrlMethod) ++
    optParam('pg_failure_url, failureUrl) ++
    optParam('pg_failure_url_method, failureUrlMethod) ++
    optParam('pg_payment_system, paymentSystem) ++
    request.additionalParams
  }
}

case class PlatronRequest
(
  merchantId: String,
  orderId: Payment.OrderId,
  amount: BigDecimal,
  description: String,
  paymentSystem: Option[String],
  currency: Option[String] = None,
  salt: String = HashHelper.uuid,
  testingMode: Boolean = true,
  userPhone: Option[String] = None,
  userContactEmail: Option[String] = None,
  requestMethod: Option[String] = None,
  checkUrl: Option[String] = None,
  resultUrl: Option[String] = None,
  successUrl: Option[String] = None,
  successUrlMethod: Option[String] = None,
  failureUrl: Option[String] = None,
  failureUrlMethod: Option[String] = None,
  additionalParams: PlatronParams = Nil
  )