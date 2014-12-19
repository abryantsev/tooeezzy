package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.db.graph.msg._
import com.tooe.core.domain._
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse}
import com.tooe.core.db.graph.{GraphGetFriendsActor, GraphException, GraphPutFriendsActor}
import scala.concurrent.Future
import com.tooe.core.usecase.session.CacheUserOnlineDataActor
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.usecase.friends_cache.FriendCacheDataActor

object FriendWriteActor {
  final val Id = Actors.FriendWrite

  case class UpdateFriendsGroups(userId: UserId, groups: Seq[UserGroupType], currentUserId: UserId)

  case class AddFriendship(friend1: UserId, friend2: UserId)

  case class DeleteFriendship(userId: UserId, currentUserId: UserId)

}

class FriendWriteActor extends AppActor with ExecutionContextProvider {
  lazy val friendCacheDataActor = lookup(FriendCacheDataActor.Id)
  lazy val putFriendsGraphActor = lookup(GraphPutFriendsActor.Id)
  lazy val getFriendsGraphActor = lookup(GraphGetFriendsActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val userEventWriteActor = lookup(UserEventWriteActor.Id)
  lazy val cacheUserOnlineDataActor = lookup(CacheUserOnlineDataActor.Id)

  import FriendWriteActor._

  def receive = {
    case UpdateFriendsGroups(userId, friendsGroups, currentUserId) =>
      val result = for {
        friendship <- putFriendsGraphActor.ask(new GraphPutUserGroups(currentUserId, userId, friendsGroups.toFriendshipType.toArray)).mapTo[GraphFriendship]
      } yield {
        val (addGroup, deleteGroup) = UserGroupType.values.partition(friendsGroups contains _)

        if(addGroup.nonEmpty) addFriendToGroups(currentUserId, userId, addGroup)
        if(deleteGroup.nonEmpty) removeFriendFromGroups(currentUserId, userId, deleteGroup)

        SuccessfulResponse
      }
      result pipeTo sender

    case AddFriendship(friend1, friend2) =>
      putFriendsToGraph(friend1, friend2) map {
        friendship =>
          userEventWriteActor ! UserEventWriteActor.NewFriendshipConfirmation(friend1, actorId = friend2)
          changeFriendshipUsersCounter(friend1, friend2, +1)
          addCacheFriend(friend1, friend2)
          addCacheFriend(friend2, friend1)
          friendship
      } pipeTo sender

    case DeleteFriendship(userId, currentUserId) =>
      (getFriendsGraphActor ? new GraphCheckFriends(currentUserId, userId)).mapTo[Boolean]
        .onSuccess {
        case true => changeFriendshipUsersCounter(userId, currentUserId, -1)
      }
      deleteFriendshipFromGraph(userId, currentUserId).recover {
        case e: GraphException => throw new NotFoundException(e.getMessage)
      }.map {
        r =>
          if (r) {
            removeCacheFriend(userId, currentUserId)
            removeCacheFriend(currentUserId, userId)
          }
          SuccessfulResponse
      }.pipeTo(sender)
  }


  def removeFriendFromGroups(currentUserId: UserId, userId: UserId, deleteGroup: Seq[UserGroupType]) {
    friendCacheDataActor ! FriendCacheDataActor.RemoveUserFromGroup(currentUserId, userId, deleteGroup)
  }

  def addFriendToGroups(currentUserId: UserId, userId: UserId, addGroup: Seq[UserGroupType]) {
    friendCacheDataActor ! FriendCacheDataActor.AddUserToGroup(currentUserId, userId, addGroup)
  }

  def putFriendsToGraph(friend1: UserId, friend2: UserId): Future[GraphFriendship] =
    (putFriendsGraphActor ? new GraphPutFriends(friend1, friend2)).mapTo[GraphFriendship]

  def deleteFriendshipFromGraph(friend1: UserId, friend2: UserId): Future[Boolean] =
    (putFriendsGraphActor ? new GraphDeleteFriends(friend1, friend2)).mapTo[Boolean]

  def addCacheFriend(friend1: UserId, friend2: UserId): Unit = {
    cacheUserOnlineDataActor ! CacheUserOnlineDataActor.AddFriend(friend1, friend2)
    friendCacheDataActor ! FriendCacheDataActor.AddUsersToFriends(friend1, Seq(friend2))
  }

  def removeCacheFriend(friend1: UserId, friend2: UserId): Unit =  {
    cacheUserOnlineDataActor ! CacheUserOnlineDataActor.RemoveFriend(friend1, friend2)
    friendCacheDataActor ! FriendCacheDataActor.RemoveUsersFromFriends(friend1, Seq(friend2))
  }

  def changeFriendshipUsersCounter(userId: UserId, currentUserId: UserId, delta: Int): Unit =
    updateStatisticActor ! UpdateStatisticActor.ChangeFriendshipCounters(Set(currentUserId, userId), delta)
}