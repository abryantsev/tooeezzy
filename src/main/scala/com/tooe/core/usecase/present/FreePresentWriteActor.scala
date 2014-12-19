package com.tooe.core.usecase.present

import com.tooe.core.application.Actors
import com.tooe.core.usecase.payment._
import com.tooe.core.usecase.AppActor
import com.tooe.core.usecase.payment.Amount
import com.tooe.core.domain.UserId
import com.tooe.core.usecase.payment.Recipient
import com.tooe.core.domain.ProductId
import com.tooe.core.usecase.payment.PaymentRequest
import com.tooe.api.service.RouteContext
import com.tooe.core.util.{HashHelper, Lang}
import com.tooe.core.db.mysql.domain.Payment

object FreePresentWriteActor {
  final val Id = Actors.FreePresentWrite
  
  case class MakeFreePresent
  (
    productId: ProductId,
    actorId: UserId,
    recipientId: UserId,
    message: String,
    isPrivate: Boolean,
    hideActor: Boolean,
    lang: Lang
    )
}

class FreePresentWriteActor extends AppActor {

  lazy val paymentWorkflowActor = lookup(PaymentWorkflowActor.Id)

  implicit val ec = context.dispatcher

  import FreePresentWriteActor._

  def receive = {
    case msg: MakeFreePresent =>
      val req = PaymentRequest(
        productId = msg.productId,
        recipient = Recipient(id = Some(msg.recipientId)),
        msg = msg.message,
        isPrivate = Some(msg.isPrivate),
        amount = Amount(BigDecimal(0), "RUR"),
        paymentSystem = PaymentSystem(system = PaymentSystem.Tooeezzy),
        responseType = ResponseType.JSON, //TODO doesn't matter here,
        pageViewType = None,
        hideActor = Some(msg.hideActor)
      )
      log.info(s"Payment is going to be created: $req")
      val future = (paymentWorkflowActor ? PaymentWorkflowActor.InitPayment(req, msg.actorId, RouteContext("", msg.lang))).mapTo[Payment]
      future onSuccess {
        case payment =>
          log.info(s"Payment has been created: $payment")
          paymentWorkflowActor ! PaymentWorkflowActor.ConfirmPayment(orderId = payment.orderId, transactionId = HashHelper.uuid, fullPrice = None)
      }
      future onFailure {
        case t =>
          log.warning(s"Payment hasn't been created because of $t for $req")
      }
  }
}
