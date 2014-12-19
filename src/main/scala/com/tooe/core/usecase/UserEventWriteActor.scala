package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.util.{Lang, DateHelper}
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse}
import scala.concurrent.Future
import com.tooe.api.validation.ValidationHelper
import com.tooe.core.usecase.promotion.PromotionDataActor
import com.tooe.core.db.mongo.domain._
import com.tooe.core.exceptions.{ApplicationException, ConflictAppException, NotFoundException, ForbiddenAppException}
import user.UserDataActor
import user_event.UserEventDataActor
import com.tooe.core.usecase.event_type.EventTypeDataActor
import com.tooe.core.usecase.location.LocationDataActor
import com.toiserver.core.usecase.notification.message.InvitationEmail
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.usecase.security.CredentialsDataActor
import com.toiserver.core.usecase.notification.domain.{Location => LocationNotification, Sender}

object UserEventWriteActor {
  final val Id = Actors.UserEventWrite

  case class NewPromotionInvitation(request: PromotionInvitationRequest, userId: UserId)
  case class NewPresentReceived(present: Present)
  case class NewFriendshipOffer(userId: UserId, actorId: UserId, friendshipRequestId: FriendshipRequestId)
  case class NewFriendshipConfirmation(userId: UserId, actorId: UserId)
  case class NewInvitation(request: InvitationRequest, actorId: UserId, lang: Lang)
  case class NewPhotoCommentReceived(comment: PhotoComment)
  case class NewNewsCommentReceived(comment: NewsComment, actorId: UserId)
  case class NewReplyNewsCommentReceived(comment: NewsComment, actorId: UserId)
  case class NewCommentOnTheWallReceived(news: News)
  case class NewNewsLikeReceived(newsLike: NewsLike, actorId: UserId)
  case class NewPhotoLikeReceived(photoLike: PhotoLike, actorId: UserId)

  case class InvitationReply(request: UserEventStatusUpdateRequest, userEventId: UserEventId, userId: UserId)
  case class PromotionInvitationReply(request: UserEventStatusUpdateRequest, userEventId: UserEventId, userId: UserId)

  case class FriendshipRequestReply(friendshipRequestId: FriendshipRequestId, userEventIdOpt: Option[UserEventId], status: UserEventStatus)
  case class DeleteUserEvent(id: UserEventId, userId: UserId)
  case class DeleteUserEvents(userId: UserId)
}

class UserEventWriteActor extends AppActor with ExecutionContextProvider{
  lazy val userEventDataActor = lookup(UserEventDataActor.Id)
  lazy val newsDataActor = lookup(NewsDataActor.Id)
  lazy val eventTypeDataActor = lookup(EventTypeDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val promotionDataActor = lookup(PromotionDataActor.Id)
  lazy val promotionVisitorWriteActor = lookup(PromotionVisitorWriteActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val credentialsDataActor = lookup(CredentialsDataActor.Id)
  lazy val notificationActor = lookup('notificationActor)

  import UserEventWriteActor._

  def receive = {
    case NewPromotionInvitation(request, userId) =>
      //TODO put in calendar if request.inCalendar
      //TODO what is the purpose of request.time?
      val future = findPromotion(request.promotionId) zip findAbsentUserIds(request.userIds :+ userId)
      future map { case (promotionOpt, absentUserIds) =>
        ValidationHelper.check(_
          .require(absentUserIds.isEmpty, "Not found users: " + absentUserIds.map(_.id).mkString(", "))
          .require(promotionOpt.isDefined, "Not found: " + request.promotionId)
        )
        val events = createPromotionInvitations(request, promotionOpt.get.location.location, userId)
        userEventDataActor ! UserEventDataActor.SaveMany(events)
        updateStatisticActor ! UpdateStatisticActor.ChangeNewUsersEventsCounters(request.userIds.toSet, 1)
        SuccessfulResponse
      } pipeTo sender

    case NewPresentReceived(present) => present.userId foreach { userId =>
      val eventPresent = UserEventPresent(
        presentId = present.id,
        productId = present.product.productId,
        locationId = present.product.locationId,
        message = present.message
      )
      val event = UserEvent(
        eventTypeId = UserEventTypeId.Present,
        userId = userId,
        createdAt = DateHelper.currentDate,
        actorId = present.actorId,
        present = Some(eventPresent)
      )
      saveUserEvent(event)
    }

    case NewCommentOnTheWallReceived(news) =>
      val event = UserEvent(
        eventTypeId = UserEventTypeId.UsersComment,
        userId = news.recipientId.get,  // recipientId should exist when news has type 'message'
        createdAt = DateHelper.currentDate,
        actorId = news.actorId,
        usersComment = news.userComment.map(uc => UserEventComment( uc.message, news.id, None))
      )
      saveUserEvent(event)

    case NewPhotoCommentReceived(comment) =>
      val future = findPhoto(comment.photoId).map(photo => {
        val event = UserEvent(
          eventTypeId = UserEventTypeId.PhotoComment,
          userId = photo.userId,
          createdAt = DateHelper.currentDate,
          actorId = Option(comment.authorId),
          photoComment = Some(UserEventPhotoComment(comment.message, comment.photoId))
        )
        saveUserEvent(event)
        SuccessfulResponse
      })
      future pipeTo sender

    case NewNewsCommentReceived(comment, actorId) =>
      getNews(comment.newsId).foreach { news =>
        val event = UserEvent(
          eventTypeId = UserEventTypeId.NewsComment,
          userId = actorId,
          createdAt = comment.creationDate,
          actorId = Option(comment.authorId),
          usersComment = Option(UserEventComment(comment.message, comment.newsId, None))
        )
        saveUserEvent(event)
      }

    case NewReplyNewsCommentReceived(comment, actorId) =>
      getNews(comment.newsId).foreach { news =>
        val event = UserEvent(
          eventTypeId = UserEventTypeId.ReplyNewsComment,
          userId = actorId,
          createdAt = comment.creationDate,
          actorId = Option(comment.authorId),
          usersComment = Option(UserEventComment(comment.message, comment.newsId, None))
        )
        saveUserEvent(event)
      }

    case NewPhotoLikeReceived(photoLike, actorId) =>
      val event = UserEvent(
        eventTypeId = UserEventTypeId.PhotoLike,
        userId = actorId,
        createdAt = DateHelper.currentDate,
        actorId = Option(photoLike.userId),
        photoLike = Some(UserEventPhotoLike(photoLike.photoId))
      )
      saveUserEvent(event)

    case NewNewsLikeReceived(newsLike, actorId) =>
      val event = UserEvent(
        eventTypeId = UserEventTypeId.NewsLike,
        userId = actorId,
        createdAt = newsLike.time,
        actorId = Option(newsLike.userId),
        newsLike = Option(UserEventNewsLike(newsLike.newsId))
      )
      saveUserEvent(event)

    case NewFriendshipOffer(userId, actorId, friendshipRequestId) =>
      val event = UserEvent(
        eventTypeId = UserEventTypeId.InviteToFriends,
        userId = userId,
        friendshipInvitation = Some(FriendshipInvitation(friendshipRequestId)),
        createdAt = DateHelper.currentDate,
        actorId = Some(actorId)
      )
      saveUserEvent(event)

    case NewFriendshipConfirmation(userId, actorId) =>
      val event = UserEvent(
        eventTypeId = UserEventTypeId.FriendshipConfirmation,
        userId = userId,
        createdAt = DateHelper.currentDate,
        actorId = Some(actorId)
      )
      saveUserEvent(event)

    case NewInvitation(request, actorId, lang) =>
      implicit val lng = lang
      val currentDate = DateHelper.currentDate
      locationDataActor.ask(LocationDataActor.GetLocation(request.locationId)).mapTo[Location]
        .map(location => {
        val invitation = Invitation(
          locationId = request.locationId,
          message = request.message,
          dateTime = request.dateTime
        )

        val events = request.userIds.map { userId =>
          UserEvent(
            eventTypeId = UserEventTypeId.Invite,
            userId = userId,
            createdAt = currentDate,
            actorId = Some(actorId),
            invitation = Some(invitation)
          )
        }

        for {
          usersMap <- getUsersMap(request.userIds :+ actorId)
          credentialsMap <- getCredentialsByUserIds(request.userIds)
        } yield {
          usersMap.values.toSeq.filter(_.id != actorId).map{ user =>
            notificationActor ! new InvitationEmail(
              credentialsMap(user.id).userName,
              credentialsMap(user.id).userName,
              usersMap.get(actorId).map(actor => new Sender(actor.name, actor.lastName, actor.gender.toString)).get,
              new LocationNotification(
                location.name.localized getOrElse "",
                location.contact.address.country,
                location.contact.address.regionName,
                location.contact.address.street,
                location.contact.phones.find(_.isMain).map(_.number).getOrElse(""), 
                location.contact.phones.find(_.isMain).map(_.countryCode).getOrElse("")
              ),
              request.dateTime.getOrElse(null), request.message.getOrElse("")
            )
          }
        }
        saveUserEvents(events)
        SuccessfulResponse
      }) pipeTo sender


    case FriendshipRequestReply(friendshipRequestId, userEventIdOpt, status) =>

      def validateUserEvent(id: UserEventId) = getUserEvent(id) map { userEvent =>
        userEvent.friendshipInvitation foreach { friendshipInvitation =>
          if (friendshipInvitation.id != friendshipRequestId) {
            throw ConflictAppException("Provided userEventId doesn't correspond to friendshipRequestId")
          }
        }
        Some(userEvent.id)
      }

      val future = for {
        userEventIdOpt <- userEventIdOpt map validateUserEvent getOrElse findFriendshipInvitationUserEventId(friendshipRequestId)
        updatedUserEventOpt <- userEventIdOpt map { id => updateUserEventOpt(id, status) } getOrElse Future.successful(None)
      } yield updatedUserEventOpt

      future onSuccess {
        case Some(userEvent) => userEventDataActor ! UserEventDataActor.UnsetFriendshipRequest(userEvent.id)
      }
      future pipeTo sender

    case DeleteUserEvent(id, userId) => getUserEvent(id) map { userEvent =>
      if (userId != userEvent.userId) {
        throw ForbiddenAppException(s"$userId is not allowed to delete foreign ${userEvent.id}")
      }
      userEventDataActor ! UserEventDataActor.Delete(id)
      updateStatisticActor ! UpdateStatisticActor.ChangeUserEventCounter(userId, -1)
      SuccessfulResponse
    } pipeTo sender

    case DeleteUserEvents(userId) =>
      userEventDataActor ! UserEventDataActor.DeleteByUserId(userId)
      updateStatisticActor ! UpdateStatisticActor.SetUserEventCounter(userId, 0)
      sender ! SuccessfulResponse

    case InvitationReply(request, userEventId, userId) =>
      val result = for {
        _ <- getUserEvent(userEventId).map(ue => if (ue.status.exists(_ == request.status)) throw ApplicationException(message = "Status is already updated") else ue)
        userEvent <- updateUserEvent(userEventId, request.status, userId)
      } yield {
        for (actorId <- userEvent.actorId) {
          val event = UserEvent(
            eventTypeId = request.status match {
              case UserEventStatus.Confirmed => UserEventTypeId.InvitationReply
              case UserEventStatus.Rejected  => UserEventTypeId.InvitationRejection
            },
            userId = actorId,
            createdAt = DateHelper.currentDate,
            actorId = Some(userId),
            invitation = userEvent.invitation
          )
          saveUserEvent(event)
        }
        SuccessfulResponse
      }
      result pipeTo sender

    case PromotionInvitationReply(request, userEventId, userId) =>
      updateUserEvent(userEventId, request.status, userId) map { userEvent =>
        for (actorId <- userEvent.actorId) {
          for (promotionInvitation <- userEvent.promotionInvitation if request.status == UserEventStatus.Confirmed) {
            promotionVisitorWriteActor ! PromotionVisitorWriteActor.GoingToPromotion(promotionInvitation.promotionId, userEvent.userId, PromotionStatus.Confirmed)
          }
          val event = UserEvent(
            eventTypeId = request.status match {
              case UserEventStatus.Confirmed => UserEventTypeId.InviteToPromotionReply
              case UserEventStatus.Rejected  => UserEventTypeId.InviteToPromotionRejection
            },
            userId = actorId,
            createdAt = DateHelper.currentDate,
            actorId = Some(userId),
            promotionInvitation = userEvent.promotionInvitation
          )
          saveUserEvent(event)
        }
        SuccessfulResponse
      } pipeTo sender
  }


  def getCredentialsByUserIds(userIds: Seq[UserId]): Future[Map[UserId, Credentials]] = {
    (credentialsDataActor ? CredentialsDataActor.GetCredentialsByUserIds(userIds)).mapTo[Seq[Credentials]].map(_.toMapId(_.userId))
  }

  def findFriendshipInvitationUserEventId(friendshipRequestId: FriendshipRequestId): Future[Option[UserEventId]] =
    (userEventDataActor ? UserEventDataActor.FindByFriendshipRequestId(friendshipRequestId)).mapTo[Option[UserEvent]] map (_ map (_.id))

  def saveUserEvent(event: UserEvent): Unit = {
    userEventDataActor ! UserEventDataActor.Save(event)
    updateStatisticActor ! UpdateStatisticActor.ChangeNewUserEventCounter(event.userId, 1)
  }

  def saveUserEvents(events: Seq[UserEvent]): Unit = userEventDataActor ! UserEventDataActor.SaveMany(events)

  def updateUserEvent(id: UserEventId, status: UserEventStatus, userId: UserId) = getUserEvent(id) flatMap { userEvent =>
    if (userEvent.userId == userId) updateUserEventOpt(id, status) map { _ getOrElse (throw NotFoundException.fromId(id)) }
    else throw ForbiddenAppException(s"$userId is not allowed to confirm or reject ${userEvent.id}")
  }

  def updateUserEventOpt(id: UserEventId, status: UserEventStatus): Future[Option[UserEvent]] =
    (userEventDataActor ? UserEventDataActor.UpdateStatus(id, status)).mapTo[Option[UserEvent]]

  def getUserEvent(id: UserEventId): Future[UserEvent] =
    (userEventDataActor ? UserEventDataActor.Get(id)).mapTo[UserEvent]

  def findAbsentUserIds(userIds: Seq[UserId]): Future[Seq[UserId]] =
    (userDataActor ? UserDataActor.FindAbsentIds(userIds)).mapTo[Seq[UserId]]

  def findPromotion(id: PromotionId): Future[Option[Promotion]] =
    (promotionDataActor ? PromotionDataActor.FindPromotion(id)).mapTo[Option[Promotion]]

  def findPhoto(photoId: PhotoId): Future[Photo] =
    photoDataActor.ask(PhotoDataActor.GetPhotoById(photoId)).mapTo[Photo]

  def createPromotionInvitations(request: PromotionInvitationRequest, locationId: LocationId, userId: UserId): Seq[UserEvent] = {

    val promotionInvitation = PromotionInvitation(
      locationId = locationId,
      promotionId = request.promotionId,
      message = request.message   //todo add message depending on gender
    )

    def createUserEvent(receiverId: UserId) = UserEvent(
      id = UserEventId(),
      userId = receiverId,
      eventTypeId = UserEventTypeId.InviteToPromotion,
      createdAt = DateHelper.currentDate,
      actorId = Some(userId),
      promotionInvitation = Some(promotionInvitation)
    )

    request.userIds map createUserEvent
  }

  def getNews(id: NewsId) =
    newsDataActor.ask(NewsDataActor.GetNews(id)).mapTo[News]

  def getUsersMap(userIds: Seq[UserId]): Future[Map[UserId, User]] = {
    userDataActor.ask(UserDataActor.GetUsers(userIds)).mapTo[Seq[User]].map(_.toMapId(_.id))
  }
}

case class PromotionInvitationRequest
(
  @JsonProperty("userids") userIds: Seq[UserId],
  @JsonProperty("promotionid") promotionId: PromotionId,
  //  @JsonProperty("time") time: Option[Date], //TODO this field is in the spec, but the purpose of it is unknown
  @JsonProperty("msg") message: Option[String],
  @JsonProperty("incalendar") inCalendar: Option[Boolean]
  ) extends UnmarshallerEntity

case class InvitationRequest
(
  @JsonProperty("userids") userIds: Seq[UserId],
  @JsonProperty("locationid") locationId: LocationId,
  @JsonProperty("time") dateTime: Option[Date],
  @JsonProperty("msg") message: Option[String],
  @JsonProperty("incalendar") inCalendar: Option[Boolean] = None  //todo this field is useless
  ) extends UnmarshallerEntity

case class UserEventStatusUpdateRequest(@JsonProperty("status") status: UserEventStatus) extends UnmarshallerEntity
