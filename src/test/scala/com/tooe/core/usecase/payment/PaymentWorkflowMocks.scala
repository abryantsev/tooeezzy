package com.tooe.core.usecase.payment

import akka.actor.{ActorSystem, Props, Actor}
import com.tooe.core.usecase.payment.PaymentWorkflowActor.CheckResult
import akka.testkit.TestActorRef
import com.tooe.core.db.mysql.services.StateChangeResult

object PaymentWorkflowMocks {

  class SuccessfulMock extends Actor {
    import PaymentWorkflowActor._
    def receive = {
      case CheckPayment(_) => sender ! CheckResult.CheckPassed
      case RejectPayment(_) => sender ! StateChangeResult.JustChanged
      case _: ConfirmPayment => sender ! StateChangeResult.JustChanged
    }
  }
  def successfulMockRef(implicit actorSystem: ActorSystem) = TestActorRef[SuccessfulMock](Props(new SuccessfulMock))

  class CheckPaymentMock(result: CheckResult) extends Actor {
    import PaymentWorkflowActor._
    def receive = {
      case CheckPayment(_) => sender ! result
    }
  }
  def checkPaymentMockRef(result: CheckResult)(implicit actorSystem: ActorSystem) =
    TestActorRef[CheckPaymentMock](Props(new CheckPaymentMock(result)))
}