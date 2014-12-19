package com.tooe.core.usecase.payment.alfabank

import com.tooe.core.usecase.ActorTestSpecification
import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, TestKit}
import com.tooe.core.util.Lang
import com.tooe.core.domain.CurrencyId
import spray.http.HttpRequest

object AlfabankHttpClientActorTest {

  def actorSystem = ActorSystem("AlfabankHttpClientActorTest", new AlfabankConfigFixture().config)
}

class AlfabankHttpClientActorTest extends ActorTestSpecification(AlfabankHttpClientActorTest.actorSystem) {

  "AlfabankHttpClientActor" should {
    import AlfabankHttpClientActor._
    "registerPreAuth" >> {
      val f = new AlfabankHttpClientActorFixture
      import f._
      val req = RegisterPreAuth(
        amount = 100.998, // third digit should be omitted
        currency = CurrencyId.RUR,
        lang = Lang.ru,
        orderNumber = BigInt("2389234073489423"),
        description = "some description that will be shown to the customer",
        pageView = PageView.Mobile,
        returnUrl = "returnUrl"
      )
      alfabankHttpClientActor ! req
      val msg = ioHttpProbe.expectMsgType[HttpRequest]
      msg.uri.toString === "http://registerpreauthurl?orderNumber=2389234073489423&returnUrl=returnUrl&description=some+description+that+will+be+shown+to+the+customer&amount=10099&sessionTimeoutSecs=900&language=ru&pageView=m&currency=810&userName=username&password=password"
    }
    "deposit" >> {
      val f = new AlfabankHttpClientActorFixture
      import f._
      alfabankHttpClientActor ! Deposit("SomeOrderUuid")
      val msg = ioHttpProbe.expectMsgType[HttpRequest]
      msg.uri.toString === "http://depositurl?userName=username&password=password&orderId=SomeOrderUuid&amount=0"
    }
    "getOrderStatusExtended" >> {
      val f = new AlfabankHttpClientActorFixture
      import f._
      alfabankHttpClientActor ! GetOrderStatusExtended("transactionId", Lang.ru)
      val msg = ioHttpProbe.expectMsgType[HttpRequest]
      msg.uri.toString === "http://getorderstatusextendedurl?userName=username&password=password&orderId=transactionId&language=ru"
    }
  }

  step {
    system.shutdown()
  }
}

class AlfabankHttpClientActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val ioHttpProbe = TestProbe()

  class AlfabankHttpClientUnderTest extends AlfabankHttpClientActor {
    override def ioHttpActorRef = ioHttpProbe.ref
  }

  lazy val alfabankHttpClientActor = TestActorRef[AlfabankHttpClientUnderTest](Props(new AlfabankHttpClientUnderTest))
}