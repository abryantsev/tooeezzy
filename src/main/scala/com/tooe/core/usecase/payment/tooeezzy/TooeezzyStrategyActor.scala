package com.tooe.core.usecase.payment.tooeezzy

import com.tooe.core.application.Actors
import com.tooe.core.usecase.{InfoMessageActor, AppActor}
import com.tooe.core.usecase.payment._
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.util.{Lang, HashHelper}
import com.tooe.core.db.mysql.services.StateChangeResult
import com.tooe.core.usecase.present.PresentDataActor
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.Present
import com.tooe.core.domain.UserId
import com.tooe.core.usecase.payment.Recipient
import com.tooe.core.exceptions.ApplicationException
import com.tooe.core.domain.ProductId

object TooeezzyStrategyActor {
  final val Id = Actors.TooeezzyStrategy

  case class RedirectTo(url: String)
  object RedirectTo {
    def apply(url: String, paymentUuid: String, lang: Lang, responseType: ResponseType): RedirectTo =
      RedirectTo(UrlSubstituteHelper(url, paymentUuid, lang, responseType))
  }
}

class TooeezzyStrategyActor extends AppActor {

  lazy val paymentWorkflowActor = lookup(PaymentWorkflowActor.Id)
  lazy val presentDataActor = lookup(PresentDataActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)

  val config = context.system.settings.config
  import config._
  val SuccessUrl = getString("payment.tooeezzy.successUrl")
  val FailureUrl = getString("payment.tooeezzy.failureUrl")

  import context.dispatcher

  import TooeezzyStrategyActor._

  def receive = {
    case InitPaymentRequest(request, routeContext, userId) =>
      val confirmPaymentFtr = for {
        _ <- checkRecipientAndActorTheSameUser(userId, request.recipient)
        _ <- preventGettingTheSameFreePresentMoreThanOneTime(userId, request.productId, routeContext.lang)
        payment <- (paymentWorkflowActor ? PaymentWorkflowActor.InitPayment(request, userId, routeContext)).mapTo[Payment]
        _ <- (paymentWorkflowActor ? PaymentWorkflowActor.ConfirmPayment(orderId = payment.orderId, transactionId = HashHelper.uuid, fullPrice = None)).mapTo[StateChangeResult]
      } yield RedirectTo(SuccessUrl, payment.uuid, routeContext.lang, request.responseType)

      confirmPaymentFtr recover {
        case t =>
          log.error(s"Payment has failed with $t")
          RedirectTo(FailureUrl, HashHelper.uuid, routeContext.lang, request.responseType)
      }
      confirmPaymentFtr pipeTo sender
  }

  def checkRecipientAndActorTheSameUser(userId: UserId, recipient: Recipient): Future[_] =
    if (Some(userId) != recipient.id) {
      Future.failed(new ApplicationException(message = s"Recipient id is mandatory '${recipient.id}' and has to be the same as current '$userId'"))
    } else Future successful ()

  def preventGettingTheSameFreePresentMoreThanOneTime(userId: UserId, productId: ProductId, lang: Lang): Future[_] =
    (presentDataActor ? PresentDataActor.FindUserPresentsByProduct(userId, productId)).mapTo[Seq[Present]] flatMap { ps =>
      if (ps.size > 0) infoMessageActor ? InfoMessageActor.GetFailure("payment_present_already_given", lang)
      else Future successful ()
    }
}