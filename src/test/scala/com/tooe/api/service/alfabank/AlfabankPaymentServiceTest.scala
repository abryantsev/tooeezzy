package com.tooe.api.service.alfabank

import akka.actor._
import spray.http._
import com.tooe.api.service.{PaymentServiceFixture, HttpServiceTest, PaymentServiceInitFixture, PaymentService}
import com.tooe.core.usecase.payment.alfabank.AlfabankStrategyActor.{ReturnUrlCallback, AlfabankInitPaymentResponse}
import akka.testkit.TestActorRef
import com.tooe.core.usecase.payment.alfabank.AlfabankStrategyActor

class AlfabankPaymentServiceTest extends HttpServiceTest {

  "PaymentService" should {
    "recognize and handle init alfabank payment requests" >> {
      val url = urlPrefix + PaymentService.Path.Root
      val f = new AlfabankPaymentServiceInitFixture
      Post(url, f.Request).withHeaders(f.sessionCookies) ~> f.route ~> check {
        status === StatusCodes.Found
        header("Location").get.value === f.initPaymentResult.url
      }
    }
    "redirect to returnUrl" >> {
      val url = urlPrefix + PaymentService.Path.status("paymentUuid") + "?orderId=orderUuid&responsetype=HTML"
      val f = new PaymentServiceFixture() {
        val returnUrl = "returnUrl"
        class PaymentMockActor extends Actor {
          def receive = {
            case ReturnUrlCallback(paymentUuid, orderUuid, lang, responseType) =>
              assert(paymentUuid == "paymentUuid")
              assert(orderUuid == "orderUuid")
              sender ! AlfabankStrategyActor.ReturnUrlCallbackResponse(returnUrl)
          }
        }
        def actorUnderTest = TestActorRef[PaymentMockActor](Props(new PaymentMockActor))

        def alfabankPaymentActor = actorUnderTest
        def platronPaymentActor = null
      }
      Get(url).withHeaders(f.sessionCookies) ~> f.route ~> check {
        status === StatusCodes.Found
        header("Location").get.value === f.returnUrl
      }
    }
  }
}

class AlfabankPaymentServiceInitFixture(implicit actorSystem: ActorSystem) extends PaymentServiceInitFixture {
  val paymentSystem = "EGATEWAY"
  def initPaymentResult = AlfabankInitPaymentResponse(url = "someurl")
  def platronPaymentActor = null
  def alfabankPaymentActor = PaymentActor
}