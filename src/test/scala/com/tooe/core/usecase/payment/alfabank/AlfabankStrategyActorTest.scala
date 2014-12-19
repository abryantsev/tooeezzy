package com.tooe.core.usecase.payment.alfabank

import com.tooe.core.usecase.ActorTestSpecification
import akka.testkit.{TestActorRef, TestKit}
import akka.actor.{Props, ActorSystem}
import scala.concurrent.Future
import com.tooe.core.util.Lang
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.usecase.payment.PaymentWorkflowActor.CheckResult
import com.tooe.core.db.mysql.services.StateChangeResult
import com.tooe.core.usecase.payment.ResponseType
import com.tooe.core.usecase.payment.alfabank.GetOrderStatusExtendedResponse.OrderStatus

object AlfabankStrategyActorTest {

  def actorSystem = ActorSystem("AlfabankStrategyActorTest", new AlfabankConfigFixture().config)
}

class AlfabankStrategyActorTest extends ActorTestSpecification(AlfabankStrategyActorTest.actorSystem) {

  "AlfabankStategyActor" should {

    import AlfabankStrategyActor._

    "InitPaymentRequest" >> {
      todo
    }

    "ReturnUrlCallback when success" >> {
      val f = new AlfabankStrategyActorFixture {
        def alfabankStategyActorFactory = new AlfabankStrategyActorSuccessful
      }
      val msg = f.alfabankStrategyActor.probeTells(ReturnUrlCallback(f.PaymentUuid, f.TransactionId, Lang.ru, ResponseType.JSON)).expectMsgType[ReturnUrlCallbackResponse]
      msg.url === "http://successUrl/ru/paymentUuid?responsetype=JSON"
    }

    "ReturnUrlCallback when getOrderStatusExtended fails" >> {
      val f = new AlfabankStrategyActorFixture {
        def alfabankStategyActorFactory = new AlfabankStrategyActorSuccessful {

          override def getOrderStatusExtended(transactionId: String, lang: Lang) = {
            assert(transactionId == TransactionId)
            Future successful GetOrderStatusExtendedResponse(orderStatus = Some(OrderStatus.PreAuthorized))
          }
        }
        //TODO check other failure possible cases
      }
      val msg = f.alfabankStrategyActor.probeTells(ReturnUrlCallback(f.PaymentUuid, f.TransactionId, Lang.ru, ResponseType.HTML)).expectMsgType[ReturnUrlCallbackResponse]
      msg.url === "http://failureUrl/ru/paymentUuid?responsetype=HTML"
    }
  }

  step {
    system.shutdown()
  }
}

abstract class AlfabankStrategyActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val OrderId: Payment.OrderId = BigInt(2304390402L)
  val PaymentUuid: String = "paymentUuid"
  val TransactionId: String = "transactionId"

  class AlfabankStrategyActorSuccessful extends AlfabankStrategyActor {

    override def deposit(orderUuid: String, lang: String) = {
      assert(orderUuid == TransactionId)
      Future successful DepositResponse("0", "message")
    }

    override def getPaymentOrderIdByUuid(orderUuid: String) = {
      assert(orderUuid == PaymentUuid)
      Future successful OrderId
    }

    override def checkPayment(orderId: Payment.OrderId) = {
      assert(orderId == OrderId)
      Future successful CheckResult.CheckPassed
    }

    override def confirmPayment(orderId: Payment.OrderId, transactionId: String) = {
      assert(orderId == OrderId)
      Future successful StateChangeResult.AlreadyChanged
    }

    override def getOrderStatusExtended(paymentUuid: String, lang: Lang) =
      Future successful GetOrderStatusExtendedResponse(orderStatus = Some(OrderStatus.Successful))
  }

  def alfabankStategyActorFactory: AlfabankStrategyActor

  lazy val alfabankStrategyActor = TestActorRef[AlfabankStrategyActor](Props(alfabankStategyActorFactory))
}