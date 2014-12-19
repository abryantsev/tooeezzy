package com.tooe.core.usecase.payment.platron

import com.tooe.core.usecase.ActorTestSpecification
import akka.actor._
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.tooe.core.usecase.payment.PaymentWorkflowActor.CheckResult
import com.tooe.core.util.HashHelper
import com.tooe.core.usecase.payment._
import com.tooe.core.usecase.payment.PlatronStrategyActor._
import com.tooe.core.payment.plantron.PlatronParams
import scala.util.Random
import com.tooe.core.payment.platron.{PlatronCheckRequestFixture, PlatronResultRequestFixture}
import com.tooe.core.usecase.payment.InitPaymentRequest
import com.tooe.core.payment.plantron.PlatronResultRequest
import com.tooe.core.usecase.payment.PlatronStrategyActor.PlatronCallback
import com.tooe.core.payment.plantron.PlatronCheckRequest
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.db.mongo.domain.UserFixture

class PlatronStrategyActorTest extends ActorTestSpecification {

  "PlatronPaymentStrategyActor InitPayment" should {
    "return PlatronInitPaymentResponse" >> {
      val f = new PaymentActorRequestFixture {
        val user = new UserFixture().user
        class UserMock extends Actor {
          def receive = {
            case UserDataActor.GetUser(userId) => sender ! user
          }
        }
        val userActorRef = TestActorRef[UserMock](Props(new UserMock))
        def paymentStrategyActorFactory = new PlatronStrategyActor(userActor = userActorRef) {
          override def initPaymentResponseSalt = "SALT"
          override lazy val SecretKey = "frozen-secret-key"
          override lazy val paymentWorkflowActor = paymentWorkflowMock
        }
        val paymentStrategyActor = TestActorRef[PlatronStrategyActor](Props(paymentStrategyActorFactory))
      }
      import f._
      paymentStrategyActor ! InitPaymentRequest(request, routeContext, userId)
      val response = expectMsgType[PlatronInitPaymentResponse]
      response !== null
      response.url === """https://www.platron.ru/payment.php?pg_merchant_id=1486&pg_salt=SALT&pg_payment_system=Payment.paymentSystem.system&pg_request_method=XML&pg_success_url_method=AUTOGET&pg_order_id=102345023&pg_sig=322b715ae72830991029a63305743607&pg_testing_mode=1&pg_user_contact_email=user%40server.domain&pg_success_url=http%3A%2F%2Ftestpay.local.tooeezzy.com%2Fv01%2Fru%2Fpayment%2Forder%2FPayment.uuid%2Fsuccess%3Fresponsetype%3DJSON&pg_failure_url=http%3A%2F%2Ftestpay.local.tooeezzy.com%2Fv01%2Fru%2Fpayment%2Forder%2FPayment.uuid%2Ffailure%3Fresponsetype%3DJSON&pg_failure_url_method=AUTOGET&pg_result_url=http%3A%2F%2Ftestpay.local.tooeezzy.com%2Fv01%2Fru%2Fpaymentaction%3Faction%3Dresult&pg_user_phone=7+5555555555&pg_description=Payment.productSnapshot.descr&pg_amount=9.11&pg_check_url=http%3A%2F%2Ftestpay.local.tooeezzy.com%2Fv01%2Fru%2Fpaymentaction%3Faction%3Dcheck"""
    }
  }

  "PlatronPaymentStrategyActor Callbacks" should {
    import PlatronStrategyActorFixture._

    "dispatch Check request to PaymentWorkflow" >> {
      val f = new ProbFixture
      f.platronStrategyActor ! PlatronCallback(script = "", platronParams = f.CheckRequest)
      f.paymentWorkflowProb expectMsg PaymentWorkflowActor.CheckPayment(f.OrderId)
      success
    }
    "dispatch Confirm request to PaymentWorkflow" >> {
      val f = new ProbFixture {
        override def succeed = true
      }
      f.platronStrategyActor ! PlatronCallback(script = "", platronParams = f.ResultRequestParams)
      f.paymentWorkflowProb expectMsg PaymentWorkflowActor.ConfirmPayment(f.OrderId, f.TransactionId, f.FullPrice)
      success
    }
    "dispatch Reject request to PaymentWorkflow" >> {
      val f = new ProbFixture {
        override def succeed = false
      }
      f.platronStrategyActor ! PlatronCallback(script = "", platronParams = f.ResultRequestParams)
      f.paymentWorkflowProb expectMsg PaymentWorkflowActor.RejectPayment(f.OrderId)
      success
    }

    "succeed when check passes" >> {
      val f = new CheckFixture(CheckResult.CheckPassed)
      val response = f.platronStrategyActor.probeTells(PlatronCallback(script = "", platronParams = f.CheckRequest))
        .expectMsgType[PlatronParams].toMap

      response.get('pg_status) === Some("ok")
      response.get('pg_sig) !== None
    }
    "reject when check Declines" >> {
      val f = new CheckFixture(CheckResult.CheckFailed)
      val response = f.platronStrategyActor.probeTells(PlatronCallback(script = "", platronParams = f.CheckRequest))
        .expectMsgType[PlatronParams].toMap

      response.get('pg_status) === Some("rejected")
      response.get('pg_sig) !== None
    }

    "succeed when result = true" >> {
      val f = new SuccessfulFixture
      val response = f.platronStrategyActor.probeTells(PlatronCallback(script = "", platronParams = f.ResultRequestParams))
        .expectMsgType[PlatronParams].toMap

      response.get('pg_status) === Some("ok")
      response.get('pg_sig) !== None
    }
    "fail when result = false" >> {
      val f = new SuccessfulFixture {
        override def succeed = false
      }
      val response = f.platronStrategyActor.probeTells(PlatronCallback(script = "", platronParams = f.ResultRequestParams))
        .expectMsgType[PlatronParams].toMap

      response.get('pg_status) === Some("rejected")
      response.get('pg_sig) !== None
    }

    "fail when signature is wrong" >> {
      val f = new SuccessfulFixture {
        override def checkSignature(script: String, platronParams: PlatronParams) = false
      }
      f.platronStrategyActor.probeTells(PlatronCallback(script = "", platronParams = f.CheckRequest))
        .expectMsgType[akka.actor.Status.Failure]
      success
    }
  }

  step {
    system.shutdown()
  }
}

object PlatronStrategyActorFixture {
  import PaymentWorkflowActor._

  class ProbFixture(implicit actorSystem: ActorSystem) extends PlatronStrategyActorFixture() {
    def userActorRef = null
    val paymentWorkflowProb = TestProbe()
    val paymentWorkflowActorRef = paymentWorkflowProb.ref
  }

  class CheckFixture(result: CheckResult)(implicit actorSystem: ActorSystem) extends PlatronStrategyActorFixture() {
    def userActorRef = null
    def paymentWorkflowActorRef = PaymentWorkflowMocks.checkPaymentMockRef(result)
  }

  class SuccessfulFixture(implicit actorSystem: ActorSystem) extends PlatronStrategyActorFixture() {
    def userActorRef = null
    def paymentWorkflowActorRef = PaymentWorkflowMocks.successfulMockRef
  }
}

abstract class PlatronStrategyActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  lazy val OrderId = BigInt(Random.nextInt)
  lazy val TransactionId = HashHelper.uuid
  val FullPrice = Some(BigDecimal(333.35))
  def succeed = true

  val PlatronResultRequestFixture = new PlatronResultRequestFixture(fullPrice = FullPrice)

  def checkRequest: PlatronCheckRequest = PlatronCheckRequestFixture.request.copy(orderId = OrderId)
  val CheckRequest: PlatronParams = PlatronCheckRequestFixture.asPlatronParams(checkRequest)

  def resultRequest: PlatronResultRequest = PlatronResultRequestFixture.request.copy(orderId = OrderId, paymentId = TransactionId, result = succeed)
  lazy val ResultRequestParams: PlatronParams = PlatronResultRequestFixture.asPlatronParams(resultRequest)

  def userActorRef: ActorRef

  def paymentWorkflowActorRef: ActorRef

  def checkSignature(script: String, platronParams: PlatronParams) = true

  def platronStrategyActorFactory = new PlatronStrategyActor(userActorRef) {
    override lazy val paymentWorkflowActor = paymentWorkflowActorRef

    override def checkSignature(script: String, platronParams: PlatronParams) =
      PlatronStrategyActorFixture.this.checkSignature(script, platronParams)
  }

  lazy val platronStrategyActor = TestActorRef[PlatronStrategyActor](Props(platronStrategyActorFactory))
}