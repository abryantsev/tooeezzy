package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem, ActorRef}
import com.tooe.core.service.{PromotionFixture, LocationFixture}
import com.tooe.core.db.mongo.domain.Location
import concurrent.Future
import com.tooe.api.service.RouteContext
import com.tooe.core.util.Lang
import com.tooe.core.domain.RegionId

class PromotionWriteActorTest extends ActorTestSpecification {

  "PromotionWriteActor" should {

    "send ChangePromotionsCounter when saving Promotion" >> {
      val f = new PromotionWriteActorCreateFixture {
        val ctx = RouteContext("v01", Lang.ru.id)
        val request = null
        val probe = TestProbe()
        override def promotionActorFactory = new PromotionWriteActor {
          override def getLocation(request: SavePromotionRequest): Future[Location] = Future successful location
          override def savePromotion(request: SavePromotionRequest, l: Location, regionId: RegionId)(implicit lang: Lang) =
            Future successful promotion
          override lazy val updateStatisticActor: ActorRef = probe.ref
        }
      }
      import f._

      promotionActor ! PromotionWriteActor.SavePromotion(request, ctx)
      probe expectMsg UpdateStatisticActor.ChangePromotionsCounter(location.contact.address.regionId, 1)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class PromotionWriteActorCreateFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val location = new LocationFixture().entity
  val promotion = new PromotionFixture(locationId = location.id).entity

  def promotionActorFactory: PromotionWriteActor

  lazy val promotionActor = TestActorRef[PromotionWriteActor](Props(promotionActorFactory))
}