package com.tooe.core.usecase.present

import com.tooe.core.usecase.ActorTestSpecification
import akka.actor.{Props, Actor, ActorSystem}
import com.tooe.core.usecase.payment.alfabank.ConfigFixture
import akka.testkit.{TestActorRef, TestKit}
import com.tooe.api.service.ObjectId
import com.tooe.core.util.DateHelper
import com.tooe.core.domain.{ProductId, UserId}

object WelcomePresentConfigHelperTest extends ConfigFixture {

  def configStr =
    """
      present.welcome {
          presenter-uid = "52ac2a20cb40301c68a1cef0"
          product-uid = "52ac2a20cb40301c68a1cef0"
          valid-from = "2013-12-14-23:59:59"
          valid-till = "2013-12-15-12:00:00"
          message = "It's a welcome present"
      }
    """

  val actorSystem = ActorSystem("FreePresentConfigHelperTest", config)
}

class WelcomePresentConfigHelperTest extends ActorTestSpecification(WelcomePresentConfigHelperTest.actorSystem) {

  "WelcomePresentConfigHelper" should {
    "provide proper config" >> {
      val f = new WelcomePresentConfigHelperFixture
      val actor = f.actorUnderTest.underlyingActor
      actor.Presenter === UserId(ObjectId("52ac2a20cb40301c68a1cef0"))
      actor.Product === ProductId(ObjectId("52ac2a20cb40301c68a1cef0"))
      actor.ValidFrom === DateHelper.parseDateTime("2013-12-14-23:59:59")
      actor.ValidTill === DateHelper.parseDateTime("2013-12-15-12:00:00")
      actor.Message === "It's a welcome present"
    }
  }

  step {
    system.shutdown()
  }
}

class WelcomePresentConfigHelperFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  class ActorUnderTestFactory extends Actor with WelcomePresentConfigHelper {
    def receive = {
      case _ =>
    }
  }

  lazy val actorUnderTest = TestActorRef[ActorUnderTestFactory](Props(new ActorUnderTestFactory))
}