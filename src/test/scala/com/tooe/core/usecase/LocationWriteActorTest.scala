package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{ActorRef, Props, ActorSystem}
import com.tooe.core.db.mongo.domain.UserFixture
import concurrent.Future
import com.tooe.core.db.graph.msg.{GraphPutLocationAcknowledgement, GraphFavorite}
import com.tooe.core.db.graph.GraphException
import com.tooe.core.service.LocationFixture
import com.tooe.core.domain._
import com.tooe.core.domain.CompanyId
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.RegionId
import com.tooe.core.domain.UserId
import com.tooe.core.util.{HashHelper, Lang}
import com.tooe.core.usecase.location.LocationDataActor

class LocationWriteActorTest extends ActorTestSpecification {

  "LocationActor" should {

    "send ChangeLocationFavoritesCounters on add/remove to favorites " >> {
      val f = new LocationWriteActorFixture {
        val location = new LocationFixture().entity
        val user = new UserFixture().user
        val locationDataActorProbe = TestProbe()
        val graphFavorite = new GraphFavorite(user.id, location.id)

        class LocationWriteActorUnderTest extends LocationWriteActor {
          override def putLocationToFavorites(userId: UserId, locationId: LocationId) =
            Future successful graphFavorite

          override def removeLocationFromFavorites(userId: UserId, locationId: LocationId) =
            Future successful true

          override def locationExistsCheck(locationId: LocationId) =
            Future successful true

          override lazy val updateStatisticActor = probe.ref
          override lazy val locationDataActor: ActorRef = locationDataActorProbe.ref
        }

        def locationActorFactory = new LocationWriteActorUnderTest
      }
      import f._

      locationActor ! LocationWriteActor.AddLocationToFavorite(location.id, user.id)
      probe expectMsg UpdateStatisticActor.ChangeLocationFavoritesCounters(location.id, user.id, 1)
      locationDataActorProbe expectMsg LocationDataActor.PutUserToUsersWhoFavorite(location.id, user.id)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class LocationWriteActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  val probe = TestProbe()

  def locationActorFactory: LocationWriteActor

  lazy val locationActor = TestActorRef[LocationWriteActor](Props(locationActorFactory))
}