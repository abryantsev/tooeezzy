package com.tooe.core.usecase.payment

import com.tooe.core.db.mysql.domain.Payment
import akka.actor.{ActorRef, Actor}
import com.tooe.core.util.{Lang, HashHelper, ActorHelper}
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.payment.plantron._
import com.tooe.core.usecase.payment
import akka.pattern.{ask, pipe}
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.payment.plantron.PlatronResponse
import com.tooe.core.db.mongo.domain.User
import com.tooe.core.payment.plantron.PlatronRequest
import com.tooe.core.exceptions.{ConflictAppException, ApplicationException}
import com.tooe.core.db.mysql.domain
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mysql.services.StateChangeResult
import scala.concurrent.Future

object PlatronStrategyActor {
  final val Id = Actors.PlatronStrategy

  case class PlatronInitPaymentResponse(url: String)

  case class PlatronCallback(script: String, platronParams: PlatronParams)
}

class PlatronStrategyActor(userActor: ActorRef) extends Actor with ActorHelper with AppActors with DefaultTimeout{
  import PlatronStrategyActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val SecretKey = settings.Platron.SecretKey

  lazy val paymentWorkflowActor = lookup(PaymentWorkflowActor.Id)

  def receive = {
    case InitPaymentRequest(request, routeContext, userId) =>
      val future = for {
        payment <- paymentWorkflowActor.ask(PaymentWorkflowActor.InitPayment(request, userId, routeContext)).mapTo[domain.Payment]
        user <- userActor.ask(UserDataActor.GetUser(userId)).mapTo[User]
      } yield PlatronInitPaymentResponse(paymentUrl(request, payment, user, routeContext.lang))
      future pipeTo sender

    case PlatronCallback(script, platronParams) =>
      if (checkSignature(script, platronParams)) {
        val future =
          if (isCheckRequest(platronParams)) sendCheckRequestFuture(platronParams, script)
          else sendResultRequestFuture(platronParams, script)
        future pipeTo sender
      }
      else {
        val message = "incorrectly signed Platron message: "+platronParams
        log.error(message)
        sender ! akka.actor.Status.Failure(ApplicationException(message = message))
      }
  }

  def checkSignature(script: String, platronParams: PlatronParams): Boolean =
    PlatronMessageSigner.checkSignature(script, platronParams, SecretKey)

  def isCheckRequest(platronParams: PlatronParams) = !platronParams.exists(_._1 == 'pg_result)

  def sendCheckRequestFuture(platronParams: PlatronParams, script: String): Future[PlatronParams] = {
    import PaymentWorkflowActor._
    val checkRequest = PlatronCheckRequestParser.parse(platronParams)
    (paymentWorkflowActor ? CheckPayment(checkRequest.orderId)).mapTo[CheckResult] map { checkResult =>
      import CheckResult._
      checkResult match {
        case CheckFailed => response(script, PlatronResponse(PlatronResponseStatus.Reject))
        case CheckPassed => response(script, PlatronResponse(PlatronResponseStatus.Ok))
      }
    }
  }

  def sendResultRequestFuture(platronParams: PlatronParams, script: String): Future[PlatronParams] = {
    import PaymentWorkflowActor._
    val resultRequest = PlatronResultRequestParser.parse(platronParams)
    val orderId = resultRequest.orderId
    import StateChangeResult._
    if (resultRequest.result)
      (paymentWorkflowActor ? ConfirmPayment(orderId, resultRequest.paymentId, resultRequest.psFullAmount)).mapTo[StateChangeResult] map {
        case JustChanged | AlreadyChanged => response(script, PlatronResponse(PlatronResponseStatus.Ok))
        case PaymentNotFound              =>
          val msg = s"Payment not found orderId=$orderId"
          log.error(msg)
          throw ConflictAppException(msg)
      }
    else
      (paymentWorkflowActor ? RejectPayment(orderId)).mapTo[StateChangeResult] map {
        case JustChanged | AlreadyChanged => response(script, PlatronResponse(PlatronResponseStatus.Reject))
        case PaymentNotFound              =>
          val msg = s"Payment not found orderId=$orderId"
          log.error(msg)
          throw ConflictAppException(msg)
      }
  }

  def response(script: String, response: PlatronResponse): PlatronParams = {
    val params = PlatronResponseBuilder.build(response)
    PlatronMessageSigner.sign(script, params, SecretKey)
  }

  def initPaymentResponseSalt: String = HashHelper.uuid

  def paymentUrl(params: PaymentRequest, entity: Payment, user: User, lang: Lang) = {
    val responseType = params.responseType
    val orderId = entity.uuid
    val request = PlatronRequest(
      salt = initPaymentResponseSalt,
      merchantId = settings.Platron.MerchantId,
      orderId = entity.orderId,
      amount = entity.productSnapshot.price,
      description = entity.productSnapshot.descr,
      testingMode = settings.Platron.TestingMode,
      userPhone = user.contact.phones.main.map(_.fullNumber),
      userContactEmail = Some(user.contact.email),
      requestMethod = Some(settings.Platron.RequestMethod),
      checkUrl = Some(settings.Platron.checkUrl(orderId, lang)),
      resultUrl = Some(settings.Platron.resultUrl(orderId, lang)),
      successUrl = Some(settings.Platron.Redirect.Success.url(responseType, orderId, lang)),
      successUrlMethod = Some(settings.Platron.Redirect.Success.method(responseType, orderId, lang)),
      failureUrl = Some(settings.Platron.Redirect.Failure.url(responseType, orderId, lang)),
      failureUrlMethod = Some(settings.Platron.Redirect.Failure.method(responseType, orderId, lang)),
      paymentSystem = Some(entity.paymentSystem.system),
      additionalParams = payment.PaymentSystem.additionalParams(params.paymentSystem)
    )
    PlatronFacade.paymentGetUrl(request, settings.Platron.SecretKey)
  }
}