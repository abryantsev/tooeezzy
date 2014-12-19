package com.tooe.core.usecase

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestProbe, TestActorRef, TestKit}
import com.tooe.core.db.mongo.domain._
import com.tooe.core.service.LocationFixture
import com.tooe.core.domain.{LocationId, UserId}
import scala.concurrent.Future

class LocationSubscriptionActorTest extends ActorTestSpecification {

  "LocationSubscriptionActor" should {

    "send ChangeLocationSubscriptionsCounter on new subscription" >> {
      val f = new LocationSubscriptionActorTestFixture {}
      import f._

      locationSubscriptionActor ! LocationSubscriptionActor.AddLocationSubscription(user.id, location.id)
      updateStatisticActorProbe expectMsg UpdateStatisticActor.UserChangeLocationSubscriptionsCounter(user.id, 1)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class LocationSubscriptionActorTestFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user = new UserFixture().user
  val location = new LocationFixture().entity.copy(lifecycleStatusId = None)
  val updateStatisticActorProbe = TestProbe()

  class LocationSubscriptionActorUnderTest extends LocationSubscriptionActor {
    override lazy val updateStatisticActor: ActorRef = updateStatisticActorProbe.ref

    override def isLocationSubscriptionExists(userId: UserId, locationId: LocationId): Future[Boolean] = Future successful(false)

    override def getLocation(id: LocationId) = Future.successful(location)
  }

  def testActorFactory = new LocationSubscriptionActorUnderTest

  lazy val locationSubscriptionActor = TestActorRef[LocationSubscriptionActorUnderTest](Props(testActorFactory))
}


