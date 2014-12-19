package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.util.{Images, Lang}
import com.tooe.core.usecase.user_event.UserEventDataActor
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.api.service.SuccessfulResponse
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.usecase.user.response.ActorItem
import com.tooe.core.exceptions.ForbiddenAppException
import com.tooe.core.usecase.event_type.EventTypeDataActor
import com.tooe.core.usecase.user.UserDataActor

object UserEventReadActor {
  final val Id = Actors.UserEventRead

  case class GetUserEvents(userId: UserId, offsetLimit: OffsetLimit, lang: Lang)
  case class GetUserEvent(userEventId: UserEventId, userId: UserId, lang: Lang)
}

class UserEventReadActor extends AppActor {
  lazy val userEventDataActor = lookup(UserEventDataActor.Id)
  lazy val userActor = lookup(UserReadActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val promotionActor = lookup(PromotionReadActor.Id)
  lazy val locationActor = lookup(LocationReadActor.Id)
  lazy val presentReadActor = lookup(PresentReadActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val eventTypeDataActor = lookup(EventTypeDataActor.Id)

  import context.dispatcher
  import UserEventReadActor._

  def receive = {
    case GetUserEvents(userId, offsetLimit, lang) =>
      findUserEvents(userId, offsetLimit) flatMap { userEvents => getUserEvents(userEvents)(lang) } map( userEvents => {
        updateStatisticActor ! UpdateStatisticActor.UpdateUserEventCounters(userId)
        UserEvents(userEvents)
      } ) pipeTo sender

    case GetUserEvent(userEventId, userId, lang) =>
      getUserEvent(userEventId) flatMap { userEvent =>
        if (userEvent.userId != userId) {
          throw ForbiddenAppException(s"$userId is not allowed to get foreign ${userEvent.id}")
        }
        getUserEvents(Seq(userEvent))(lang) map UserEvents
      } pipeTo sender
  }

  def getUserEvents(userEvents: Seq[UserEvent])(implicit lang: Lang): Future[Seq[UserEventItem]] = {
    val actorMapFtr = {
      val actorIds = userEvents.flatMap(_.actorId).toSet
      findActors(actorIds, Images.Userevents.Full.Actor.Media) map (_.toMapId(_.userId))
    }

    val presentsMapFtr = {
      val presentIds = userEvents.flatMap(_.present).map(_.presentId).toSet
      findPresents(presentIds) map (_.toMapId(_.id))
    }

    val promotionsMapFtr = {
      val promotionIds = userEvents.flatMap(_.promotionInvitation).map(_.promotionId).toSet
      findPromotions(promotionIds) map (_.toMapId(_.id))
    }

    val locationsMapFtr = {
      val locationIds = userEvents.flatMap(_.getLocationId).toSet
      findLocations(locationIds) map (_.toMapId(_.id))
    }

    val photoMapFtr = {
      val photoIds = userEvents.flatMap(_.photoLike.map(_.photoId)).toSet
      findPhotos(photoIds) map (_.map(x => x.id -> UserEventPhotoItem(x)).toMap)
    }

    val newsMapFtr = {
      val eventTypeIds = userEvents.map(_.eventTypeId).distinct.map(ue => UserEventTypeId.userEventTypeId2EventTypeId(ue))
      val withNewsUserEvents = userEvents.filter(ue => ue.getNewsId.isDefined)
      for {
        eventTypes <- getEventTypes(eventTypeIds)
        usersMap <- getUsers(withNewsUserEvents.map(_.userId) ++ withNewsUserEvents.flatMap(_.actorId)).mapTo[Seq[User]].map(_.toMapId(_.id))
      } yield {
        withNewsUserEvents.map(ue => UserEventNews(ue, eventTypes.toMapId(_.id), usersMap)).toMapId(_.newsId)
      }
    }

    for {
      actorMap <- actorMapFtr
      promotionMap <- promotionsMapFtr
      locationMap <- locationsMapFtr
      presentMap <- presentsMapFtr
      photoMap <- photoMapFtr
      newsMap <- newsMapFtr
    } yield userEvents map UserEventItem(actorMap, promotionMap, locationMap, presentMap, photoMap, newsMap)
  }

  def getEventTypes(eventTypeIds: Seq[EventTypeId]): Future[Seq[EventType]] = {
    eventTypeDataActor.ask(EventTypeDataActor.GetEventTypes(eventTypeIds)).mapTo[Seq[EventType]]
  }

  def getUserEvent(id: UserEventId): Future[UserEvent] =
    (userEventDataActor ? UserEventDataActor.Get(id)).mapTo[UserEvent]

  def findUserEvents(userId: UserId, offsetLimit: OffsetLimit): Future[Seq[UserEvent]] =
    (userEventDataActor ? UserEventDataActor.FindByUserId(userId, offsetLimit)).mapTo[Seq[UserEvent]]

  def findActors(ids: Set[UserId], imageSize: String)(implicit lang: Lang): Future[Seq[ActorItem]] =
    (userActor ? UserReadActor.FindActors(ids, lang, imageSize)).mapTo[Seq[ActorItem]]

  def getUsers(ids: Seq[UserId]): Future[Seq[User]] =
    (userDataActor ? UserDataActor.GetUsers(ids)).mapTo[Seq[User]]

  def findPromotions(ids: Set[PromotionId])(implicit lang: Lang): Future[Seq[UserEventPromotion]] =
    (promotionActor ? PromotionReadActor.FindUserEventPromotions(ids, lang)).mapTo[Seq[UserEventPromotion]]

  def findLocations(ids: Set[LocationId])(implicit lang: Lang): Future[Seq[UserEventLocation]] =
    (locationActor ? LocationReadActor.FindUserEventLocations(ids, lang, Images.Userevents.Full.Location.Media)).mapTo[Seq[UserEventLocation]]

  def findPresents(ids: Set[PresentId]): Future[Seq[UserEventPresentItem]] =
    (presentReadActor ? PresentReadActor.GetUserEventPresents(ids)).mapTo[Seq[UserEventPresentItem]]

  def findPhotos(photoIds: Set[PhotoId]): Future[Seq[Photo]] =
    photoDataActor.ask(PhotoDataActor.GetPhotos(photoIds.toSeq)).mapTo[Seq[Photo]]
}

case class UserEvents(@JsonProperty("userevents") userEvents: Seq[UserEventItem]) extends SuccessfulResponse

case class UserEventItem
(
  @JsonProperty("id") id: UserEventId,
  @JsonProperty("time") createdAt: Date,
  @JsonProperty("type") eventTypeId: UserEventTypeId,
  @JsonProperty("actor") actor: Option[ActorItem],
  @JsonProperty("message") message: Option[UserEventMessage],
  @JsonProperty("friendshiprequest") friendship: Option[FriendshipInvitationItem],
  @JsonProperty("present") present: Option[UserEventPresentItem],
  @JsonProperty("location") location: Option[UserEventLocation],
  @JsonProperty("date") date: Option[UserEventDate],
  @JsonProperty("promotion") promotion: Option[UserEventPromotion],
  @JsonProperty("photo") photo: Option[UserEventPhotoItem],
  @JsonProperty("status") status: Option[UserEventStatus],
  @JsonProperty("news") news: Option[UserEventNews]
  )

object UserEventItem {

  def apply
  (
    actorMap: Map[UserId, ActorItem],
    promotionMap: Map[PromotionId, UserEventPromotion],
    locationMap: Map[LocationId, UserEventLocation],
    presentMap: Map[PresentId, UserEventPresentItem],
    photoMap: Map[PhotoId, UserEventPhotoItem],
    newsMap: Map[NewsId, UserEventNews]
  )(ue: UserEvent): UserEventItem = {
    val promotionIdOpt = ue.promotionInvitation map (_.promotionId)
    val promotionOpt = promotionIdOpt flatMap promotionMap.get
    val presentIdOpt = ue.present map (_.presentId)
    UserEventItem(
      id = ue.id,
      createdAt = ue.createdAt,
      eventTypeId = ue.eventTypeId,
      actor = ue.actorId flatMap actorMap.get,
      message = ue.getMessage map UserEventMessage,
      friendship = ue.friendshipInvitation map FriendshipInvitationItem.apply,
      present = presentIdOpt flatMap presentMap.get,
      date = ue.getDate map UserEventDate,
      location = ue.getLocationId flatMap locationMap.get,
      promotion = promotionOpt,
      photo = ue.getPhotoIdOpt flatMap photoMap.get,
      status = ue.getStatus,
      news = ue.getNewsId.map(newsId => newsMap(newsId))
    )
  }
}

case class UserEventMessage(@JsonProperty("msg") msg: String)

case class UserEventDate
(
  @JsonProperty("date") date: Date
  //@JsonProperty("time") time: Date //TODO no time field yet, see #1631
)

case class FriendshipInvitationItem(id: FriendshipRequestId)

object FriendshipInvitationItem {
  def apply(friendshipInvitation: FriendshipInvitation): FriendshipInvitationItem =
    FriendshipInvitationItem(id = friendshipInvitation.id)
}