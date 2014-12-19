package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem}
import com.tooe.core.service._
import com.tooe.core.domain._
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain._
import com.tooe.core.usecase.user.response.ActorItem
import com.tooe.core.util.Lang
import com.tooe.core.domain.PromotionId
import com.tooe.core.domain.PresentId
import com.tooe.core.domain.LocationId
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.UserId
import com.tooe.core.domain.PromotionId
import com.tooe.core.domain.PresentId
import com.tooe.core.domain.LocationId
import com.tooe.core.db.mongo.domain.EventType
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.UserEvent
import com.tooe.core.domain.PhotoId
import com.tooe.core.domain.EventTypeId

class UserEventReadActorTest extends ActorTestSpecification {

  "UserEventReadActor" should {
    "return user events" >> {
      val f = new UserEventActorGetEventsFixture {
        def userEventTypeId = UserEventTypeId.InviteToPromotion
      }
      import f._

      val userEvents = userEventReadActor.underlyingActor.getUserEvents(Seq(userEvent))(lang).awaitResult
      val ue = userEvents.head

      ue.eventTypeId === UserEventTypeId.InviteToPromotion
      ue.actor.get === f.actorItem
      ue.location.get === f.userEventLocation
      ue.message.get.msg === f.userEvent.getMessage.get

      //TODO here testing all variants together
      ue.promotion.get === f.userEventPromotion
      ue.present.get === f.userEventPresentItem
    }
    "findPresents" >> {
      val f = new UserEventReadActorFixture {
        val probe = TestProbe()
        def userEventReadActorFactory = new UserEventReadActor {
          override lazy val presentReadActor = probe.ref
        }
        val presentIds = Set(PresentId())
      }
      import f._
      userEventReadActor.underlyingActor.findPresents(presentIds)
      probe expectMsg PresentReadActor.GetUserEventPresents(presentIds)
      success
    }

    "update user statistic when read events" >> {
      val f = new UserEventActorReadEventsFixture {
        val probe = TestProbe()
        override def userEventReadActorFactory = new UserEventReadActorUnderTest {
          override lazy val updateStatisticActor = probe.ref
        }
      }
      import f._

      userEventReadActor ! UserEventReadActor.GetUserEvents(user.id, null, null)
      probe expectMsg UpdateStatisticActor.UpdateUserEventCounters(user.id)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class UserEventReadActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  def userEventReadActorFactory: UserEventReadActor

  lazy val userEventReadActor = TestActorRef[UserEventReadActor](Props(userEventReadActorFactory))
}


abstract class UserEventActorGetEventsFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  def userEventTypeId: UserEventTypeId

  val user = new UserFixture().user
  val actorItem = ActorItem(_ => None, "")(user)

  val lang = Lang.ru

  val location = new LocationFixture().entity
  val userEventLocation = UserEventLocation(lang, "100x100")(location)

  val promotion = new PromotionFixture(location.id).entity
  val userEventPromotion = UserEventPromotion(lang)(promotion)

  val userEventPresentItem = UserEventPresentItem(PresentFixture.present())

  val photo = new PhotoFixture().photo
  val eventType = new EventTypeFixture().eventType

  val userEvent = new UserEventFixture(
    location.locationId,
    promotion.id,
    user.id,
    userEventPresentItem.id,
    photo = photo
  ).userEvent.copy(eventTypeId = userEventTypeId)

  class UserEventReadActorUnderTest extends UserEventReadActor {
    override def findUserEvents(userId: UserId, offsetLimit: OffsetLimit): Future[Seq[UserEvent]] =
      Future successful Seq(userEvent)

    override def findActors(ids: Set[UserId], imageSize: String)(implicit lang: Lang): Future[Seq[ActorItem]] =
      Future successful Seq(actorItem)

    override def findPromotions(ids: Set[PromotionId])(implicit lang: Lang): Future[Seq[UserEventPromotion]] =
      Future successful Seq(userEventPromotion)

    override def findLocations(ids: Set[LocationId])(implicit lang: Lang): Future[Seq[UserEventLocation]] =
      Future successful Seq(userEventLocation)

    override def findPresents(ids: Set[PresentId]): Future[Seq[UserEventPresentItem]] =
      Future successful Seq(userEventPresentItem)

    override def findPhotos(photoIds: Set[PhotoId]): Future[Seq[Photo]] =
      Future successful Seq(photo)

    override def getEventTypes(eventTypeIds: Seq[EventTypeId]): Future[Seq[EventType]] =
      Future successful Seq(eventType)

    override def getUsers(ids: Seq[UserId]): Future[Seq[User]] =
      Future successful Seq(user)
  }
  class UserEventWriteActorUnderTest extends UserEventWriteActor {

    override def findPhoto(photoId: PhotoId): Future[Photo] =
      Future successful photo
  }

  def userEventReadActorFactory = new UserEventReadActorUnderTest
  def userEventWriteActorFactory = new UserEventWriteActorUnderTest

  lazy val userEventReadActor = TestActorRef[UserEventReadActorUnderTest](Props(userEventReadActorFactory))
  lazy val userEventWriteActor = TestActorRef[UserEventWriteActorUnderTest](Props(userEventWriteActorFactory))
}

abstract class UserEventActorReadEventsFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user = new UserFixture().user

  val userEvent = new UserEventFixture(
    userId = user.id
  ).userEvent
  val userEventItem = new UserEventItemFixture().userEventItem

  class UserEventReadActorUnderTest extends UserEventReadActor {

    override def findUserEvents(userId: UserId, offsetLimit: OffsetLimit): Future[Seq[UserEvent]] =
      Future successful Seq(userEvent)

    override def getUserEvents(userEvents: Seq[UserEvent])(implicit lang: Lang): Future[Seq[UserEventItem]] = {
      Future successful Seq(userEventItem)
    }
  }
  def userEventReadActorFactory = new UserEventReadActorUnderTest

  lazy val userEventReadActor = TestActorRef[UserEventReadActorUnderTest](Props(userEventReadActorFactory))

}

class UserEventItemFixture {
  val userEventItem = UserEventItem(
      id = null,
      createdAt = null,
      eventTypeId = null,
      actor = None,
      message = None,
      friendship = None,
      present = None,
      location = None,
      date = None,
      promotion = None,
      photo = None,
      status = None,
      news = None
    )
}