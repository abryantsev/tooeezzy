package com.tooe.api.service.platron

import akka.actor._
import spray.http._
import akka.testkit.{TestActorRef, TestKit}
import com.tooe.api.service.{HttpServiceTest, PaymentServiceInitFixture, PaymentService}
import com.tooe.core.usecase.payment.PlatronStrategyActor.PlatronCallback
import com.tooe.core.payment.plantron
import com.tooe.core.util.XmlHelpers._
import com.tooe.core.usecase.payment.PlatronStrategyActor
import com.tooe.core.util.HashHelper

class PlatronPaymentServiceTest extends HttpServiceTest {

  "PaymentService" should {
    val url = urlPrefix + PaymentService.Path.Root

    "Platron initPayment" >> {
      val f = new PaymentServiceInitFixture {
        val paymentSystem = "whatever-platron-should-handle"
        val initPaymentResult = PlatronStrategyActor.PlatronInitPaymentResponse(url = HashHelper.uuid)
        def platronPaymentActor = PaymentActor
        def alfabankPaymentActor = null
      }
      Post(url, f.Request).withHeaders(f.sessionCookies) ~> f.route ~> check {
        status === StatusCodes.Found
        header("Location").get.value === f.initPaymentResult.url
      }
    }

    //TODO might also check what message it sends to a decent actor
  }

  "PlatronPaymentStrategy" should {
    val url = urlPrefix+PaymentService.Path.Callback+"?system=something"
    "dispatch callbacks to the decent actor and get responses back" >> {
      val f = new PlatronPaymentServiceCallbackFixture() {
        class PlatronPaymentStrategyMock extends Actor {
          def receive = {
            case PlatronCallback(script, platronParams) => sender ! Response
          }
        }
        val platronPaymentStrategyActorRef = TestActorRef[PlatronPaymentStrategyMock](Props(new PlatronPaymentStrategyMock))
      }
      Post(url, f.WellStructuredRequest) ~> f.route ~> check {
        status === StatusCodes.OK
        body.asString.asXml === f.ResponseXml.asXml
      }
    }
    "don't answer for incorrectly signed messages" >> {
      todo
    }
    "reject when failure" >> {
      todo
    }
  }
}

abstract class PlatronPaymentServiceCallbackFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val Salt = "c8556278"
  val Response: plantron.PlatronParams = Seq('pg_salt -> Salt)
  val ResponseXml = "<response><pg_salt>"+Salt+"</pg_salt></response>"

  val WellStructuredRequest = "pg_xml=%3C%3Fxml+version%3D%221.0%22+encoding%3D%22utf-8%22%3F%3E%0A%3Crequest%3E%3Cpg_salt%3Ec8556278%3C%2Fpg_salt%3E%3Cpg_order_id%3E2342342%3C%2Fpg_order_id%3E%3Cpg_payment_id%3E5672516%3C%2Fpg_payment_id%3E%3Cpg_amount%3E9.9900%3C%2Fpg_amount%3E%3Cpg_currency%3ERUR%3C%2Fpg_currency%3E%3Cpg_ps_amount%3E9.99%3C%2Fpg_ps_amount%3E%3Cpg_ps_full_amount%3E9.99%3C%2Fpg_ps_full_amount%3E%3Cpg_ps_currency%3ERUR%3C%2Fpg_ps_currency%3E%3Cpg_payment_system%3ETEST%3C%2Fpg_payment_system%3E%3Cinternal_order_id%3ETODO%3C%2Finternal_order_id%3E%3Cpg_sig%3E8d4bf7766909b4036d1b9962ee226907%3C%2Fpg_sig%3E%3C%2Frequest%3E"

  def platronPaymentStrategyActorRef: ActorRef

  lazy val service = new PaymentService {
    override lazy val platronStrategyActor = platronPaymentStrategyActorRef
  }

  lazy val route = service.route
}