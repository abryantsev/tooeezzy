package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.db.graph.msg._
import com.tooe.core.domain._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.db.mongo.domain.{FriendshipRequest, UserEvent, User}
import scala.concurrent.Future
import com.tooe.core.domain.UserId
import com.tooe.core.util.{DateHelper, Lang, InfoMessageHelper}
import user.UserDataActor
import com.tooe.core.usecase.friendshiprequest.FriendshipRequestDataActor
import com.tooe.core.exceptions.{ConflictAppException, ForbiddenAppException}
import scala.util.Success
import com.tooe.core.db.graph.GraphGetFriendsActor

object FriendshipRequestWriteActor {
  final val Id = Actors.FriendshipRequestWrite

  case class OfferFriendship(userId: UserId, currentUserId: UserId, lang: Lang)

  case class AcceptOrRejectFriendship(request: AcceptOrRejectFriendshipRequest, friendshipRequestId: FriendshipRequestId, userId: UserId)

}

class FriendshipRequestWriteActor extends AppActor with ExecutionContextProvider {

  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val friendshipRequestDataActor = lookup(FriendshipRequestDataActor.Id)
  lazy val friendWriteActor = lookup(FriendWriteActor.Id)
  lazy val newsWriteActor = lookup(NewsWriteActor.Id)
  lazy val graphGetFriendsActor = lookup(GraphGetFriendsActor.Id)

  import FriendshipRequestWriteActor._

  def receive = {
    case OfferFriendship(userId, currentUserId, lang) =>
      isFriends(userId, currentUserId).flatMap {
        case true => throw new ConflictAppException(message = "Users are already friends.")
        case _ =>
          findFriendshipRequest(currentUserId, actorId = userId) flatMap {
            case Some(friendshipRequest) => replyFriendshipRequest(friendshipRequest, None, UserEventStatus.Confirmed)
            case None =>
              for {
                _ <- preventUserSendFriendshipRequestToStar(userId, actorId = currentUserId, lang = lang)
                _ <- preventDuplicatingFriendRequest(userId, actorId = currentUserId)
                friendshipRequestId <- saveFriendshipRequest(userId, actorId = currentUserId) map (_.id)
              } yield {
                userEventWriteActor ! UserEventWriteActor.NewFriendshipOffer(userId, actorId = currentUserId, friendshipRequestId)
                changeFriendshipRequestCount(userId, 1)
                SuccessfulResponse
              }
          }
      } pipeTo sender

    case AcceptOrRejectFriendship(request, friendshipRequestId, userId) =>
      val future = for {
        friendshipRequest <- getFriendshipRequest(friendshipRequestId)
        _ <- checkFriendshipRequest(friendshipRequest, userId)
        result <- replyFriendshipRequest(friendshipRequest, request.userEventId, request.status)
      } yield result
      future pipeTo sender
  }

  def isFriends(currentUserId: UserId, userId: UserId): Future[Boolean] =
    graphGetFriendsActor.ask(new GraphCheckFriends(currentUserId, userId)).mapTo[Boolean]

  def replyFriendshipRequest(friendshipRequest: FriendshipRequest, userEventIdOpt: Option[UserEventId], status: UserEventStatus): Future[SuccessfulResponse] =
    for {
      _ <- friendshipRequestReply(friendshipRequest.id, userEventIdOpt, status)
      _ <- status match {
        case UserEventStatus.Rejected => Future successful()
        case UserEventStatus.Confirmed => addFriendship(friend1 = friendshipRequest.actorId, friend2 = friendshipRequest.userId).andThen {
          case Success(_) => newsWriteActor ! NewsWriteActor.AddFriendShipNews(friendshipRequest.actorId, friendshipRequest.userId)
        }
      }
    } yield {
      friendshipRequestDataActor ! FriendshipRequestDataActor.Delete(friendshipRequest.id)
      changeFriendshipRequestCount(friendshipRequest.userId, -1)
      SuccessfulResponse
    }

  def friendshipRequestReply(friendshipRequestId: FriendshipRequestId, userEventIdOpt: Option[UserEventId], status: UserEventStatus): Future[Option[UserEvent]] =
    (userEventWriteActor ? UserEventWriteActor.FriendshipRequestReply(friendshipRequestId, userEventIdOpt, status)).mapTo[Option[UserEvent]]

  def findFriendshipRequest(userId: UserId, actorId: UserId): Future[Option[FriendshipRequest]] =
    (friendshipRequestDataActor ? FriendshipRequestDataActor.FindRequest(userId, actorId = actorId)).mapTo[Option[FriendshipRequest]]

  def preventDuplicatingFriendRequest(userId: UserId, actorId: UserId): Future[_] =
    findFriendshipRequest(userId, actorId = actorId) map (friendshipRequestOpt =>
      friendshipRequestOpt map(_ => throw ConflictAppException("Duplicating friendship request")))

  def checkFriendshipRequest(friendshipRequest: FriendshipRequest, userId: UserId): Future[_] = Future {
    if (friendshipRequest.userId != userId) {
      throw new ForbiddenAppException("It's forbidden to confirm or reject another user's friendship request")
    }
  }

  def preventUserSendFriendshipRequestToStar(userId: UserId, actorId: UserId, lang: Lang): Future[_] =
    getUsers(userId, actorId) map {
      users =>
        if (users.toMapId(_.id)(actorId).star.isEmpty && users.toMapId(_.id)(userId).star.isDefined) {
          InfoMessageHelper.throwAppExceptionById("regular_user_can't_send_friendship_request_to_star")(lang)
        }
    }

  def saveFriendshipRequest(userId: UserId, actorId: UserId): Future[FriendshipRequest] = {
    val request = FriendshipRequest(userId = userId, actorId = actorId, createdAt = DateHelper.currentDate)
    (friendshipRequestDataActor ? FriendshipRequestDataActor.Save(request)).mapTo[FriendshipRequest]
  }

  def getFriendshipRequest(id: FriendshipRequestId): Future[FriendshipRequest] =
    (friendshipRequestDataActor ? FriendshipRequestDataActor.Get(id)).mapTo[FriendshipRequest]

  def changeFriendshipRequestCount(userId: UserId, delta: Int): Unit =
    updateStatisticActor ! UpdateStatisticActor.ChangeFriendshipRequestCounter(userId, delta)

  def addFriendship(friend1: UserId, friend2: UserId): Future[GraphFriendship] =
    (friendWriteActor ? FriendWriteActor.AddFriendship(friend1, friend2)).mapTo[GraphFriendship]

  def getUsers(userId: UserId, currentUserId: UserId): Future[Seq[User]] =
    userDataActor.ask(UserDataActor.GetUsers(Seq(userId, currentUserId))).mapTo[Seq[User]]
}

case class FriendshipInvitationRequest(@JsonProperty("userid") userId: UserId) extends UnmarshallerEntity

case class UpdateFriendsGroupsRequest(usergroups: Seq[UserGroupType]) extends UnmarshallerEntity

case class AcceptOrRejectFriendshipRequest
(
  @JsonProperty("status") status: UserEventStatus,
  @JsonProperty("usereventid") userEventId: Option[UserEventId]
  ) extends SuccessfulResponse