package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.db.graph.msg._
import scala.collection.JavaConverters._
import com.tooe.core.domain._
import org.bson.types.ObjectId
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service.{ExecutionContextProvider, OffsetLimit, SuccessfulResponse}
import com.tooe.core.db.graph.GraphGetFriendsActor
import session.CacheUserOnlineDataActor
import com.tooe.core.db.mongo.domain.{User, CacheUserOnline}
import concurrent.Future
import com.tooe.core.db.graph.domain.FriendshipType
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.user.UserDataActor.{CountFindAndFilterUsers, FindAndFilterUsersBy}
import com.tooe.core.usecase.session.CacheUserOnlineDataActor.FindOnlineUsers
import com.tooe.core.util.Images

object FriendReadActor {
  final val Id = Actors.FriendRead

  case class GetUserFriendsOnline(userId: UserId, offsetLimit: OffsetLimit, entities: Set[FriendField])

  case class SearchAmongOwnFriends(request: SearchAmongOwnFriendsRequest, userId: UserId, offsetLimit: OffsetLimit)

  case class SearchUserFriends(request: SearchUserFriendsRequest, userId: UserId, offsetLimit: OffsetLimit)

  case class GetUserFriends(userId: UserId)

  case class AreFriends(currentUserId: UserId, userId: UserId)

  case class GetOnlineFriendsNumber(userId: UserId)

  case class GetFriendshipGroups(currentUserId: UserId, userId: UserId)

}

trait FriendReadComponent {
  self: AppActor with ExecutionContextProvider =>

  lazy val friendReadActor = lookup(FriendReadActor.Id)

  import FriendReadActor._

  def getUserFriends(userId: UserId): Future[Set[UserId]] = (friendReadActor ? GetUserFriends(userId)).mapTo[Set[UserId]]

  def areFriends(currentUserId: UserId, userId: UserId): Future[Boolean] =
    (friendReadActor ? AreFriends(currentUserId, userId)).mapTo[Boolean]

  def getOnlineFriendsNumber(userId: UserId): Future[Int] =
    (friendReadActor ? GetOnlineFriendsNumber(userId)).mapTo[Int]

  def getFriendshipGroups(currentUserId: UserId, userId: UserId): Future[Set[FriendshipType]] =
    (friendReadActor ? GetFriendshipGroups(currentUserId, userId)).mapTo[Set[FriendshipType]]
}

class FriendReadActor extends AppActor with ExecutionContextProvider with FriendCacheReadComponent {

  lazy val getFriendGraphActor = lookup(GraphGetFriendsActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val cacheUserOnlineDataActor = lookup(CacheUserOnlineDataActor.Id)

  import FriendReadActor._

  def receive = {
    case GetUserFriendsOnline(userId, offsetLimit, entities) =>
      val result = for {
        fields <- Future.successful(entities)
        count <- fields
          .find(_ == FriendField.UsersCount)
          .map(_ => countOnlineFriends(userId).map(Option(_)))
          .getOrElse(Future.successful(None))
        users <- fields
          .find(_ == FriendField.Users)
          .map(_ => getOnlineFriendsIds(userId, offsetLimit).flatMap(getUsers).map(Option(_)))
          .getOrElse(Future.successful(None))
      } yield GetUserFriendsOnlineResponse(users.map(_.map(GetUserFriendsOnlineResponseItem(Images.Friends.Full.User.Media))), count)
      result.pipeTo(sender)

    case SearchAmongOwnFriends(request, currentUserId, offsetLimit) =>
      lazy val friends = getUserFriendsViaCache(currentUserId, request.getUserGroup)
      val result = for {
        fields <- Future(request.entities.getOrElse(FriendField.values))
        count <- fields
          .find(_ == FriendField.UsersCount)
          .map(_ => friends.flatMap(countFilteredUserFriends(_, request)).map(Option(_)))
          .getOrElse(Future.successful(None))
        users <- fields
          .find(_ == FriendField.Users)
          .map(_ => friends.flatMap(getFilteredUserFriends(_, request, offsetLimit)).flatMap(getUserData(_, Images.Friendssearch.Full.User.Media)).map(Option(_)))
          .getOrElse(Future.successful(None))
      } yield SearchFriendsResponse(users, count)
      result.pipeTo(sender)

    case SearchUserFriends(request, userId, offsetLimit) =>
      def filterUserFtr(userIds: Seq[UserId]) = userDataActor.ask(UserDataActor.SearchUserFriends(userIds, request, offsetLimit)).mapTo[Seq[User]]
      def userCountFtr(userIds: Seq[UserId]) = userDataActor.ask(UserDataActor.CountSearchUserFriends(userIds, request)).mapTo[Long]
      (for {
        userIds <- getUserFriendsViaCache(userId, FriendshipType.FRIEND)
        (users, count) <- getUserDataOpt(userIds, request.entities, filterUserFtr(userIds), Images.Userfriendssearch.Full.User.Media) zip
          countUsers(request.entities, userCountFtr(userIds))
      } yield SearchFriendsResponse(users, count)) pipeTo sender

    case GetUserFriends(userId) => getUserFriends(userId) pipeTo sender

    case AreFriends(currentUserId, userId) =>
      val future = for {
        isFriendsFromCache <- findCacheUserOnline(currentUserId) map (_ map (_.friends contains userId))
        result <- isFriendsFromCache map (r => Future successful r) getOrElse isFriends(currentUserId, userId)
      } yield result
      future pipeTo sender

    case GetOnlineFriendsNumber(userId) => getOnlineFriends(userId) map (_.size) pipeTo sender

    case GetFriendshipGroups(currentUserId, userId) =>
      (getFriendGraphActor ? new GraphGetFriendship(currentUserId, userId)).mapTo[GraphFriendship] map {
        friendship =>
          friendship.getUsergroups.asScala.toSet
      } pipeTo sender
  }

  def getFilteredUserFriends(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest, offsetLimit: OffsetLimit) =
    userDataActor.ask(FindAndFilterUsersBy(userIds, request, offsetLimit)).mapTo[Seq[User]]

  def countFilteredUserFriends(userIds: Seq[UserId], request: SearchAmongOwnFriendsRequest) =
    userDataActor.ask(CountFindAndFilterUsers(userIds, request)).mapTo[Long]

  def getUserFriends(userId: UserId): Future[Set[UserId]] =
    for {
      cacheUserOnline <- findCacheUserOnline(userId)
      friendIds <- cacheUserOnline map (Future successful _.friends) getOrElse getFriends(userId, FriendshipType.FRIEND)
    } yield friendIds.toSet

  def getOnlineFriends(userId: UserId): Future[Set[UserId]] =
    for {
      friends <- getUserFriends(userId)
      onlineFriends <- getOnlineUsers(friends.toSeq)
    } yield onlineFriends

  def findCacheUserOnline(userId: UserId): Future[Option[CacheUserOnline]] =
    (cacheUserOnlineDataActor ? CacheUserOnlineDataActor.FindCacheUserOnline(userId)).mapTo[Option[CacheUserOnline]]

  def countUsers(entities: Option[Set[FriendField]], countUserFtr: Future[Long]): Future[Option[Long]] = {
    if (entities.map(_.contains(FriendField.UsersCount)).getOrElse(true))
      countUserFtr.map(Some.apply)
    else
      Future.successful(None)
  }

  def getUserDataOpt(userIds: Seq[UserId], entities: Option[Set[FriendField]], loadDataFtr: Future[Seq[User]], imageResponseSize: String): Future[Option[Seq[UserData]]] = {
    if (entities.map(_.contains(FriendField.Users)).getOrElse(true))
      for {
        users <- loadDataFtr
        userData <- getUserData(users, imageResponseSize)
      }
      yield {
        Some(userData)
      }
    else
      Future.successful(None)
  }

  def getFriends(userId: UserId, friendGroup: FriendshipType): Future[Seq[UserId]] =
    (getFriendGraphActor ? new GraphGetFriends(userId, friendGroup)).mapTo[GraphFriends] map {
      graphFriends =>
        graphFriends.getFriends.asScala.toSeq
    }

  def isFriends(currentUserId: UserId, userId: UserId): Future[Boolean] =
    (getFriendGraphActor ? new GraphCheckFriends(currentUserId, userId)).mapTo[Boolean]

  def getUserData(users: Seq[User], imageResponseSize: String): Future[Seq[UserData]] =
    getOnlineUsers(users map (_.id)) map {
      onlineUserIds => users map UserData(onlineUserIds.toSet, imageResponseSize)
    }

  def getOnlineUsers(userIds: Seq[UserId]): Future[Set[UserId]] =
    (cacheUserOnlineDataActor ? FindOnlineUsers(userIds)).mapTo[Set[UserId]]

  def getOnlineFriendsIds(userId: UserId, offsetLimit: OffsetLimit) =
    cacheUserOnlineDataActor.ask(CacheUserOnlineDataActor.GetOnlineFriends(userId, offsetLimit)).mapTo[Seq[UserId]]

  def countOnlineFriends(userId: UserId) =
    cacheUserOnlineDataActor.ask(CacheUserOnlineDataActor.CountOnlineFriends(userId)).mapTo[Long]

  def getUsers(ids: Seq[UserId]) =
    userDataActor.ask(UserDataActor.GetUsers(ids)).mapTo[Seq[User]]
}


case class GetFriendsRequest(userId: UserId, view: Option[ViewType])

case class SearchAmongOwnFriendsRequest
(
  name: Option[String],
  country: Option[String],
  @JsonProperty("usersgroup") usersGroup: Option[UserGroupType],
  entities: Option[Set[FriendField]]
  ) {
  def getUserGroup: FriendshipType = usersGroup.map(_.toFriendshipType) getOrElse FriendshipType.FRIEND
}

case class SearchUserFriendsRequest
(
  name: Option[String],
  entities: Option[Set[FriendField]]
  )

case class GetUserFriendsOnlineResponse(users: Option[Seq[GetUserFriendsOnlineResponseItem]],
                                        @JsonProperty("userscount") usersCount: Option[Long]) extends SuccessfulResponse

case class GetUserFriendsOnlineResponseItem(
                                             id: UserId,
                                             name: String,
                                             @JsonProperty("lastname") lastName: String,
                                             @JsonProperty("secondname") secondName: Option[String],
                                             media: MediaUrl,
                                             address: AddressShort
                                             )

object GetUserFriendsOnlineResponseItem {
  def apply(imageResponseSize: String)(u: User): GetUserFriendsOnlineResponseItem =
    GetUserFriendsOnlineResponseItem(
      u.id,
      u.name,
      u.lastName,
      u.secondName,
      u.getMainUserMediaUrl(imageResponseSize),
      AddressShort(
        u.contact.address.country,
        u.contact.address.regionName
      ))
}

case class SearchFriendsResponse(users: Option[Seq[UserData]] = None,
                                 @JsonProperty("userscount") usersCount: Option[Long] = None) extends SuccessfulResponse

case class UserData
(
  id: ObjectId,
  name: String,
  @JsonProperty("lastname") lastName: String,
  @JsonProperty("secondname") secondName: Option[String],
  media: MediaUrl,
  address: AddressShort,
  @JsonProperty("isonline") isOnline: Option[Boolean] = None
  )

object UserData {
  def apply(onlineUsers: Set[UserId], imageResponseSize: String)(u: User): UserData =
    UserData(
      u.id.id,
      u.name,
      u.lastName,
      u.secondName,
      u.getMainUserMediaUrl(imageResponseSize),
      AddressShort(
        u.contact.address.country,
        u.contact.address.regionName
      ),
      isOnline = if (onlineUsers contains u.id) Some(true) else None
    )
}