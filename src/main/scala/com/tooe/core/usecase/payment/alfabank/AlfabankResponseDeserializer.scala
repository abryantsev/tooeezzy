package com.tooe.core.usecase.payment.alfabank

import com.tooe.core.util.JsonDeserializer

object AlfabankResponseDeserializer extends AlfabankResponseDeserializer {

  val deserializer = JsonDeserializer
}

trait AlfabankResponseDeserializer {

  def deserializer: JsonDeserializer

  def registerPreAuthResponse(source: String): RegisterPreAuthResponse = {
    val map = deserializer.deserializeMap(source)
    RegisterPreAuthResponse(
      formUrl = map.get("formUrl") map { case v: String => v },
      transactionId = map.get("orderId") map { case v: String => v },
      errorCode = map.get("errorCode").map{ case v: String => v },
      errorMessage = map.get("errorMessage").map{ case v: String => v }
    )
  }

  def depositResponse(source: String): DepositResponse = {
    val map = deserializer.deserializeMap(source)
    DepositResponse(
      errorCode = map.get("errorCode").map{ case v: String => v }.get,
      errorMessage = map.get("errorMessage").map{ case v: String => v }.get
    )
  }

  def getOrderStatusExtendedResponse(source: String): GetOrderStatusExtendedResponse = {
    import GetOrderStatusExtendedResponse._

    val map = deserializer.deserializeMap(source)

    GetOrderStatusExtendedResponse(
      orderStatus = map.get("orderStatus") map { case v: Int => OrderStatus(v) },
      errorCodeOpt = map.get("ErrorCode") orElse map.get("errorCode") map { case v: String => ErrorCode(v) },
      errorMessage = map.get("ErrorMessage") orElse map.get("errorMessage") map { case v: String => v }
    )
  }
}