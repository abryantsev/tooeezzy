package com.tooe.core.usecase.payment.alfabank

case class RegisterPreAuthResponse
(
  formUrl: Option[String] = None,
  transactionId: Option[String] = None,
  errorCode: Option[String] = None,
  errorMessage: Option[String] = None
  )

object RegisterPreAuthResponse {
  object Successful {
    def unapply(resp: RegisterPreAuthResponse): Option[String] = resp.formUrl
  }
}

case class DepositResponse(errorCode: String, errorMessage: String) {
  def successful: Boolean = errorCode == "0"
}

object GetOrderStatusExtendedResponse {
  case class OrderStatus(id: Int)
  object OrderStatus {
    val Registered = OrderStatus(0)
    val PreAuthorized = OrderStatus(1)
    val Successful = OrderStatus(2)
    val Canceled = OrderStatus(3)
    val Refunded = OrderStatus(4)
    val AcsAuthorizationInitialized = OrderStatus(5)
    val AuthorizationDeclined = OrderStatus(6)
  }

  case class ErrorCode(id: String)
  object ErrorCode {
    val NoError = ErrorCode("0")
  }

  object PaymentSucceeded {
    def unapply(resp: GetOrderStatusExtendedResponse): Option[GetOrderStatusExtendedResponse] =
      if (resp.errorCode == ErrorCode.NoError && resp.orderStatus == Some(OrderStatus.Successful)) Some(resp)
      else None
  }
}

case class GetOrderStatusExtendedResponse
(
  orderStatus: Option[GetOrderStatusExtendedResponse.OrderStatus] = None,
  errorCodeOpt: Option[GetOrderStatusExtendedResponse.ErrorCode] = None,
  errorMessage: Option[String] = None
  ) {
  import GetOrderStatusExtendedResponse._

  def errorCode = errorCodeOpt getOrElse ErrorCode.NoError
}