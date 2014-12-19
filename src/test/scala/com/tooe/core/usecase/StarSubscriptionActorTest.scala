package com.tooe.core.usecase

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import com.tooe.core.db.mongo.domain.UserFixture

class StarSubscriptionActorTest extends ActorTestSpecification {

  "StarSubscriptionActor" should {

    "send ChangeUserStarSubscriptionsCounter on new subscription" >> {
      val f = new StarSubscriptionActorTestFixture {}
      import f._

      starSubscriptionActor ! StarSubscriptionActor.Subscribe(user.id, starUser.id)
      testProbe expectMsg UpdateStatisticActor.ChangeUserStarSubscriptionsCounter(user.id, 1)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class StarSubscriptionActorTestFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user = new UserFixture().user.copy(star = None)
  val starUser = new UserFixture().user
  val testProbe = TestProbe()

  class StarSubscriptionActorUnderTest extends StarSubscriptionActor {
    override lazy val updateStatisticActor: ActorRef = testProbe.ref
  }

  def testActorFactory = new StarSubscriptionActorUnderTest

  lazy val starSubscriptionActor = TestActorRef[StarSubscriptionActorUnderTest](Props(testActorFactory))
}




