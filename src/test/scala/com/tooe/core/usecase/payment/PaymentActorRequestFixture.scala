package com.tooe.core.usecase.payment

import akka.actor._
import akka.testkit.TestActorRef
import org.bson.types.ObjectId
import com.tooe.core.domain.{ProductId, UserId}
import com.tooe.api.service.RouteContext
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.service.PaymentFixture
import com.tooe.core.util.Lang
import com.tooe.core.domain.CountryId

abstract class PaymentActorRequestFixture(implicit system: ActorSystem) {

  def paymentWorkflowResult: Payment = {
    val payment = new PaymentFixture {
      override def paymentUuid = "Payment.uuid"
    }.payment
    payment.orderJpaId = BigInt(102345023L).bigInteger
    payment
  }

  import com.tooe.core.util.HashHelper.uuid
  import com.tooe.core.util.SomeWrapper._

  val routeContext = RouteContext("v01", Lang.ru) //TODO doubling, already have it somewhere
  val userId = UserId()

  val request = PaymentRequest(
    productId = ProductId(new ObjectId()),
    recipient = Recipient(
      email = uuid.take(50),
      phone = uuid.take(20),
      country = CountryParams(
        id = CountryId(uuid.take(2)),
        phone = uuid.take(10)
      )
    ),
    msg = uuid,
    isPrivate = Option(true),
    amount = Amount(BigDecimal(9.99), "RUR"),
    paymentSystem = PaymentSystem(system = "oerghsklvj3ri0j"),
    responseType = ResponseType.JSON,
    pageViewType = None
  )

//  class SecurityCacheMock extends Actor {
//    val userId = UserId(new ObjectId)
//
//    def receive = {
//      case GetUserId(_) =>
//        //TODO assert on arguments
//        sender ! userId
//    }
//  }

//  lazy val securityCacheMock = TestActorRef[SecurityCacheMock](Props(new SecurityCacheMock))

  class PaymentWorkflowMock extends Actor {
    def receive = {
      case PaymentWorkflowActor.InitPayment(req, userId, ctx) =>
        //TODO assert on arguments
        assert(userId != null)
        assert(ctx == routeContext)
        sender ! paymentWorkflowResult
    }
  }

  lazy val paymentWorkflowMock = TestActorRef[PaymentWorkflowMock](Props(new PaymentWorkflowMock))
}