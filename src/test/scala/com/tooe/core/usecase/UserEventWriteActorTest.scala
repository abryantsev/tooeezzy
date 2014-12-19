package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.tooe.core.service._
import com.tooe.core.usecase.user_event.UserEventDataActor
import com.tooe.core.domain._
import akka.actor.{Props, ActorSystem}
import com.tooe.core.db.mongo.domain.{Promotion, UserEvent, UserFixture}
import concurrent.Future

class UserEventWriteActorTest extends ActorTestSpecification {

  "UserEventWriteActor" should {
    "create event on InviteToPromotion" >> {
      wontdo // covered by gatling test
    }
    "create event on NewPresentReceived" >> {
      val f = new UserEventWriteActorFixture {
        val probe = TestProbe()
        def userEventWriteActorFactory = new UserEventWriteActor {
          override lazy val userEventDataActor = probe.ref
        }
      }
      val present = PresentFixture.present().copy(hideSender = None)
      f.userEventWriteActor ! UserEventWriteActor.NewPresentReceived(present)

      val msg = f.probe.expectMsgType[UserEventDataActor.Save]
      val entity = msg.entity
      entity.userId === present.userId.get
      entity.eventTypeId === UserEventTypeId.Present
      entity.createdAt !== null
      entity.status === None
      entity.actorId === present.actorId
      val p = entity.present.get
      p.presentId === present.id
      p.productId === present.product.productId
      p.locationId === present.product.locationId
      p.message === present.message
      entity.getMessage === present.message
    }
    "create event on NewPhotoLikeReceived" >> {
      val f = new UserEventActorGetEventsFixture {
        def userEventTypeId = UserEventTypeId.PhotoLike

        val probe = TestProbe()
        override def userEventWriteActorFactory = new UserEventWriteActorUnderTest {
          override lazy val userEventDataActor = probe.ref
        }
      }
      import f._

      val photoLike = new PhotoLikeFixture().photoLike
      val actor = UserId()
      f.userEventWriteActor ! UserEventWriteActor.NewPhotoLikeReceived(photoLike, actor)

      val msg = f.probe.expectMsgType[UserEventDataActor.Save]
      val entity = msg.entity
      entity.eventTypeId === UserEventTypeId.PhotoLike
      entity.createdAt !== null
      entity.status === None
      entity.userId === actor
      entity.actorId === Some(photoLike.userId)
      val p = entity.photoLike.get
      p.photoId === photoLike.photoId

      val userEvents = f.userEventReadActor.underlyingActor.getUserEvents(Seq(userEvent))(lang).awaitResult
      val ue = userEvents.head

      ue.eventTypeId === UserEventTypeId.PhotoLike
      ue.actor.get === f.actorItem

      ue.photo === Option(f.photo).map(UserEventPhotoItem(_))
    }

    "hide actor on NewPresentReceived" >> {
      val f = new UserEventWriteActorFixture {
        val probe = TestProbe()
        def userEventWriteActorFactory = new UserEventWriteActor {
          override lazy val userEventDataActor = probe.ref
        }
      }
      val present = PresentFixture.present().copy(hideSender = Some(true))
      f.userEventWriteActor ! UserEventWriteActor.NewPresentReceived(present)

      val msg = f.probe.expectMsgType[UserEventDataActor.Save]
      msg.entity.actorId === None
    }

    "update statistic on DeleteUserEvents" >> {
      val f = new UserEventWriteActorFixture {
        val probe = TestProbe()
        def userEventWriteActorFactory = new UserEventWriteActor {
          override lazy val updateStatisticActor = probe.ref
        }
      }
      import f._
      userEventWriteActor ! UserEventWriteActor.DeleteUserEvents(user.id)

      probe expectMsg UpdateStatisticActor.SetUserEventCounter(user.id, 0)
      success
    }
    "update statistic on DeleteUserEvent" >> {
      val f = new UserEventWriteActorFixture {
        val probe = TestProbe()
        def userEventWriteActorFactory = new UserEventWriteActorUnderTest {
          override lazy val updateStatisticActor = probe.ref
        }
      }
      import f._
      userEventWriteActor ! UserEventWriteActor.DeleteUserEvent(userEvent.id, user.id)

      probe expectMsg UpdateStatisticActor.ChangeUserEventCounter(user.id, -1)
      success
    }
    "update newEvents statistic counter on saving new event" >> {
      val f = new UserEventWriteActorFixture {
        val probe = TestProbe()
        def userEventWriteActorFactory = new UserEventWriteActorUnderTest {
          override lazy val updateStatisticActor = probe.ref
        }
      }
      import f._
      userEventWriteActor.underlyingActor.saveUserEvent(userEvent)

      probe expectMsg UpdateStatisticActor.ChangeNewUserEventCounter(user.id, 1)
      success
    }
    "update newEvents statistic counter on NewPromotionInvitation" >> {
      val f = new UserEventWriteActorNewPromotionInvitationFixture {
        val probe = TestProbe()
        def userEventWriteActorFactory = new UserEventWriteActorUnderTest {
          override lazy val updateStatisticActor = probe.ref
        }
      }
      import f._
      userEventWriteActor ! UserEventWriteActor.NewPromotionInvitation(request, user.id)

      probe expectMsg UpdateStatisticActor.ChangeNewUsersEventsCounters(Set(requestUser.id), 1)
      success
    }
  }

  step {
    system.shutdown()
  }
}


abstract class UserEventWriteActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user = new UserFixture().user
  val userEvent = new UserEventFixture(
    userId = user.id
  ).userEvent

  class UserEventWriteActorUnderTest extends UserEventWriteActor {

    override def getUserEvent(id: UserEventId): Future[UserEvent] =
      Future successful userEvent

  }

  def userEventWriteActorFactory: UserEventWriteActor

  lazy val userEventWriteActor = TestActorRef[UserEventWriteActor](Props(userEventWriteActorFactory))
}

abstract class UserEventWriteActorNewPromotionInvitationFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user = new UserFixture().user
  val requestUser = new UserFixture().user

  val userEvent = new UserEventFixture(
    userId = user.id
  ).userEvent

  val promotion = new PromotionFixture().entity
  val request = PromotionInvitationRequest(Seq(requestUser.id),promotion.id, None, None)

  class UserEventWriteActorUnderTest extends UserEventWriteActor {

    override def findPromotion(id: PromotionId): Future[Option[Promotion]] = Future successful Some(promotion)

    override def findAbsentUserIds(userIds: Seq[UserId]): Future[Seq[UserId]] =
      Future successful Seq()

  }

  def userEventWriteActorFactory: UserEventWriteActor

  lazy val userEventWriteActor = TestActorRef[UserEventWriteActorUnderTest](Props(userEventWriteActorFactory))
}



