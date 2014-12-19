package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem}
import com.tooe.core.service.{PromotionFixture, LocationFixture}
import com.tooe.core.db.mongo.domain.{PromotionVisitor, Promotion}
import concurrent.Future
import com.tooe.core.util.DateHelper
import com.tooe.core.domain.{PromotionStatus, UserId, PromotionId}

class PromotionVisitorWriteActorTest extends ActorTestSpecification {

  "PromotionVisitorWriteActor" should {

    "GoingToPromotion" >> {
      def future(promotionStatus: PromotionStatus) = new PromotionVisitorWriteActorCreateFixture {
        val userId = UserId()
        val probe = TestProbe()
        override def promotionActorFactory = new PromotionVisitorWriteActor {
          override def promotionVisitorStatusHasChanged(promotionId: PromotionId, userId: UserId, status: PromotionStatus): Unit =
            probe.ref ! "promotionHasChanged"

          override def getPromotion(id: PromotionId): Future[Promotion] = Future successful promotion

          override def findPromotionVisitor(promotionId: PromotionId, userId: UserId) =
            Future successful Some(PromotionVisitor(
              promotion = promotionId,
              visitor = userId,
              status = promotionStatus,
              time = DateHelper.currentDate
            ))
        }
      }
      "call promotionVisitorStatusHasChanged when status has changed" >> {
        val f = future(PromotionStatus.Confirmed)
        import f._
        promotionActor ! PromotionVisitorWriteActor.GoingToPromotion(promotion.id, userId, PromotionStatus.Rejected)
        probe expectMsg "promotionHasChanged"
        success
      }
      "don't call promotionVisitorStatusHasChanged when status hasn't change" >> {
        val f = future(PromotionStatus.Confirmed)
        import f._
        promotionActor ! PromotionVisitorWriteActor.GoingToPromotion(promotion.id, userId, PromotionStatus.Confirmed)
        probe expectNoMsg ()
        success
      }
    }
  }

  step {
    system.shutdown()
  }
}

abstract class PromotionVisitorWriteActorCreateFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val location = new LocationFixture().entity
  val promotion = new PromotionFixture(locationId = location.id).entity

  def promotionActorFactory: PromotionVisitorWriteActor

  lazy val promotionActor = TestActorRef[PromotionVisitorWriteActor](Props(promotionActorFactory))
}