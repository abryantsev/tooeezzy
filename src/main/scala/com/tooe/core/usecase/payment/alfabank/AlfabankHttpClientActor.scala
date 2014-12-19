package com.tooe.core.usecase.payment.alfabank

import com.tooe.core.domain.CurrencyId
import com.tooe.core.util.Lang
import com.tooe.core.db.mysql.domain.Payment
import spray.http.{HttpCharsets, Uri, HttpResponse}
import akka.io.IO
import spray.can.Http
import spray.httpx.RequestBuilding._
import com.tooe.core.usecase.AppActor
import java.net.URLEncoder
import HttpCharsets.`UTF-8`

object AlfabankHttpClientActor {

  case class RegisterPreAuth
  (
    amount: BigDecimal,
    currency: CurrencyId,
    lang: Lang,
    orderNumber: Payment.OrderId,
    description: String,
    pageView: PageView,
    returnUrl: String
    )

  sealed trait PageView {
    def code: String
  }
  object PageView {
    case object Mobile extends PageView {
      val code = "m"
    }
    case object Desktop extends PageView {
      val code = "d"
    }
  }

  case class Deposit(transactionId: String)

  case class GetOrderStatusExtended(paymentUuid: String, lang: Lang)

}

class AlfabankHttpClientActor extends AppActor {

  implicit val system = context.system
  implicit val ec = context.dispatcher

  val config = context.system.settings.config
  import config._
  val Username = getString("payment.alfabank.username")
  val Password = getString("payment.alfabank.password")

  val RegisterPreAuthUrl = getString("payment.alfabank.registerPreAuthUrl")
  val DepositUrl = getString("payment.alfabank.depositUrl")
  val GetOrderStatusExtendedUrl = getString("payment.alfabank.getOrderStatusExtendedUrl")

  val PaymentTimeoutSecs = (getMilliseconds("payment.alfabank.timeout") / 1000) min 1200

  val respDeserializer = AlfabankResponseDeserializer

  import AlfabankHttpClientActor._

  def receive = {
    case req: RegisterPreAuth =>

      val params: Map[String, String] = Map(
        "amount" -> (req.amount * 100).toInt.toString,
        "currency" -> CurrencyId.code(req.currency).toString,
        "language" -> req.lang.id,
        "orderNumber" -> req.orderNumber.toString,
        "userName" -> Username,
        "password" -> Password,
        "returnUrl" -> req.returnUrl,
        "description" -> req.description,
        "sessionTimeoutSecs" -> PaymentTimeoutSecs.toString,
        "pageView" -> req.pageView.code
      )

      val uri = Uri(RegisterPreAuthUrl + "?" + renderUrlParams(params))

      (ioHttpActorRef ? Get(uri).withHostHeader).mapTo[HttpResponse] map { resp =>
        log.info("Get: {}\nResponse: {}", uri, resp)
        respDeserializer.registerPreAuthResponse(resp.entity asString `UTF-8`)
      } pipeTo sender

    case Deposit(transactionId) =>
      val params: Map[String, String] = Map(
        "userName" -> Username,
        "password" -> Password,
        "orderId" -> transactionId,
        "amount" -> "0" // 0 means total order amount should be deposit
      )
      val uri = Uri(DepositUrl + "?" + renderUrlParams(params))
      (ioHttpActorRef ? Get(uri).withHostHeader).mapTo[HttpResponse] map { resp =>
        log.info("Get: {}\nResponse: {}", uri, resp)
        respDeserializer.depositResponse(resp.entity asString `UTF-8`)
      } pipeTo sender

    case GetOrderStatusExtended(transactionId, lang) =>
      val params: Map[String, String] = Map(
        "userName" -> Username,
        "password" -> Password,
        "orderId" -> transactionId,
        "language" -> lang.id
      )
      val uri = Uri(GetOrderStatusExtendedUrl + "?" + renderUrlParams(params))
      (ioHttpActorRef ? Get(uri).withHostHeader).mapTo[HttpResponse] map { resp =>
        log.info("Get: {}\nResponse: {}", uri, resp)
        respDeserializer.getOrderStatusExtendedResponse(resp.entity asString `UTF-8`)
      } pipeTo sender

  }

  def renderUrlParams(params: Map[String, String]): String =
    params map { case (k, v) => k + "=" + URLEncoder.encode(v) } mkString "&"

  def ioHttpActorRef = IO(Http)
}