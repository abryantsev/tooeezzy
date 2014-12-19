package com.tooe.core.usecase

import com.tooe.core.usecase.payment.ResponseType
import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import com.tooe.api._
import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, HasIdentity, UnmarshallerEntity}
import java.net.URLEncoder

object PaymentResultUrlReadActor {

  final val Id = Actors.PaymentResultUrlRead

  case class GetPaymentResultUrl(status: PaymentCompleteStatus, responseType: ResponseType, lang: Lang)
}

class PaymentResultUrlReadActor extends AppActor {

  implicit val ec = context.dispatcher

  val config = context.system.settings.config
  import config._
  val htmlResultUrl = getString("payment.html-result-url")

  val infoMessageData = lookup(Actors.InfoMessage)

  import PaymentResultUrlReadActor._

  def receive = {
    case GetPaymentResultUrl(status, responseType, lang) =>
      val msgId = status match {
        case PaymentCompleteStatus.Ok   => "payment_success_redirect"
        case PaymentCompleteStatus.Error => "payment_error_redirect"
      }

      getMessage(msgId, lang) map { message =>
        responseType match {
          case ResponseType.JSON => PaymentCompleteJson(status, message)
          case ResponseType.HTML => PaymentCompleteHtml(url = htmlResultUrl(message, status))
        }
      } pipeTo sender
  }

  def getMessage(msgId: String, lang: Lang) = (infoMessageData ? InfoMessageActor.GetMessage(msgId, lang)).mapTo[String]

  def htmlResultUrl(msg: String, status: PaymentCompleteStatus): String = {
    val encodedMsg = URLEncoder.encode(msg)
    s"$htmlResultUrl?status=${status.id}&message=$encodedMsg"
  }
}

case class PaymentCompleteHtml(url: String)

case class PaymentCompleteJson
(
  @JsonProp("status") status: PaymentCompleteStatus,
  message: String
  ) extends UnmarshallerEntity

trait PaymentCompleteStatus extends HasIdentity {
  def id: String
}

object PaymentCompleteStatus extends HasIdentityFactoryEx[PaymentCompleteStatus] {
  object Ok extends PaymentCompleteStatus {
    def id: String = "ok"
  }
  object Error extends PaymentCompleteStatus {
    def id: String = "error"
  }
  val values = Ok :: Error :: Nil
}