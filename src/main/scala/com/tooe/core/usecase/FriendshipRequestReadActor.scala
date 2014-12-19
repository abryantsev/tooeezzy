package com.tooe.core.usecase

import com.tooe.api.service.{SuccessfulResponse, OffsetLimit, ExecutionContextProvider}
import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.core.usecase.friendshiprequest.FriendshipRequestDataActor
import com.fasterxml.jackson.annotation.JsonProperty
import scala.concurrent.Future
import com.tooe.core.util.Images
import com.tooe.core.db.mongo.domain.FriendshipRequest
import com.tooe.core.domain.AddressShort
import com.tooe.core.usecase.UserReadActor.GetSearchUsersDto
import com.tooe.core.domain.FriendshipRequestId
import com.tooe.core.domain.UserId
import com.tooe.core.domain.MediaUrl

object FriendshipRequestReadActor {
  final val Id = Actors.FriendshipRequestRead

  case class IncomingFriendshipRequests(userId: UserId, offsetLimit: OffsetLimit)
  case class GetFriendshipInvitationStatus(userId: UserId, actorId: UserId)
}

class FriendshipRequestReadActor extends AppActor with ExecutionContextProvider {

  lazy val friendshipRequestDataActor = lookup(FriendshipRequestDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  import FriendshipRequestReadActor._

  def receive = {
    case IncomingFriendshipRequests(userId, offsetLimit) =>
      val future = for {
        entities <- findUsersIncomingFriendshipRequests(userId, offsetLimit)
        userIds = entities.map(_.actorId).toSet
        userItems <- getSearchUsers(userIds)
        userItemsMap = userItems toMapId (_.id)
      } yield FriendshipRequestsResponse(entities map FriendshipRequestItem(userItemsMap))
      future pipeTo sender

    case GetFriendshipInvitationStatus(userId, actorId) =>
      def checkIfUserHasAlreadyInvited = isFriendshipRequestExist(actorId = actorId, userId = userId) map {
        exists => if (exists) Some(FriendshipStatus.IsInvited) else None
      }
      def checkIfUserHasBeenInvited = isFriendshipRequestExist(actorId = userId, userId = actorId) map {
        exists => if (exists) Some(FriendshipStatus.InviteMe) else None
      }
      checkIfUserHasAlreadyInvited flatMap { r1 =>
        if (r1.isDefined) Future successful r1
        else checkIfUserHasBeenInvited
      } pipeTo sender
  }

  def findUsersIncomingFriendshipRequests(userId: UserId, offsetLimit: OffsetLimit): Future[Seq[FriendshipRequest]] =
    (friendshipRequestDataActor ? FriendshipRequestDataActor.FindByUser(userId, offsetLimit)).mapTo[Seq[FriendshipRequest]]

  def getSearchUsers(userIds: Set[UserId]): Future[Seq[SearchUsersItemDto]] =
    (userReadActor ? GetSearchUsersDto(userIds.toSeq, Images.Friendshiprequests.Full.User.Media)).mapTo[Seq[SearchUsersItemDto]]

  def isFriendshipRequestExist(userId: UserId, actorId: UserId): Future[Boolean] =
    findFriendshipRequest(userId, actorId) map (_.isDefined)

  def findFriendshipRequest(userId: UserId, actorId: UserId): Future[Option[FriendshipRequest]] =
    (friendshipRequestDataActor ? FriendshipRequestDataActor.FindRequest(userId, actorId = actorId)).mapTo[Option[FriendshipRequest]]
}

case class FriendshipRequestsResponse
(
  @JsonProperty("friendshiprequests") items: Seq[FriendshipRequestItem]
  ) extends SuccessfulResponse

case class FriendshipRequestItem
(
  @JsonProperty("id") id: FriendshipRequestId,
  @JsonProperty("userid") userId: UserId,
  @JsonProperty("name") name: String,
  @JsonProperty("lastname") lastname: String,
  @JsonProperty("media") media: MediaUrl,
  @JsonProperty("address") address: AddressShort
  )

object FriendshipRequestItem {
  def apply(userItemsMap: Map[UserId, SearchUsersItemDto])(entity: FriendshipRequest): FriendshipRequestItem = {
    val userItem = userItemsMap(entity.actorId)
    FriendshipRequestItem(
      id = entity.id,
      userId = userItem.id,
      name = userItem.name,
      lastname = userItem.lastName,
      media = userItem.mediaUrl,
      address = userItem.address
    )
  }
}