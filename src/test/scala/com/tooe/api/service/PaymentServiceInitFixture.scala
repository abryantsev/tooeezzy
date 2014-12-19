package com.tooe.api.service

import akka.actor._
import com.tooe.core.usecase.payment
import akka.testkit.{TestKit, TestActorRef}
import com.tooe.core.usecase.payment.InitPaymentRequest
import com.tooe.core.domain.ProductId

abstract class PaymentServiceInitFixture(implicit actorSystem: ActorSystem) extends PaymentServiceFixture {

  def responseType: payment.ResponseType = payment.ResponseType.JSON
  def paymentSystem: String

  def initPaymentResult: Any
  def alfabankPaymentActor: ActorRef
  def platronPaymentActor: ActorRef

  class PaymentMockActor extends Actor {
    var requestMessage: InitPaymentRequest = null

    def receive = {
      case m: InitPaymentRequest =>
        requestMessage = m
        sender ! initPaymentResult
    }
  }

  lazy val PaymentActor = TestActorRef[PaymentMockActor](Props(new PaymentMockActor))

  import com.tooe.core.util.HashHelper.uuid

  lazy val Request = payment.PaymentRequest(
    productId = ProductId(ObjectId()),
    recipient = payment.Recipient(email = Some(uuid)),
    msg = uuid,
    amount = payment.Amount(BigDecimal(9.99), "RUR"),
    paymentSystem = payment.PaymentSystem(system = paymentSystem),
    responseType = responseType,
    pageViewType = None
  )
}

abstract class PaymentServiceFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) with SessionCookieFixture {

  def alfabankPaymentActor: ActorRef
  def platronPaymentActor: ActorRef

  lazy val service = new PaymentService with ServiceAuthMock {
    override lazy val platronStrategyActor = platronPaymentActor
    override lazy val alfabankStrategyActor = alfabankPaymentActor
  }

  lazy val route = service.route
}