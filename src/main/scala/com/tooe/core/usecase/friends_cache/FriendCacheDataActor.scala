package com.tooe.core.usecase.friends_cache

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.core.util.ActorHelper
import com.tooe.core.domain.{UserGroupType, UserId}
import concurrent.Future
import akka.pattern.pipe
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.FriendsCacheDataService
import com.tooe.core.db.mongo.domain.FriendsCache
import com.tooe.core.db.graph.domain.FriendshipType

object FriendCacheDataActor {
  final val Id = Actors.FriendDataCache

  case class Find(id: UserId, friendsGroup: FriendshipType)

  case class Save(id: UserId, friendsGroup: FriendshipType, friendsIds: Seq[UserId])

  case class AddUsersToFriends(userId: UserId, friends: Seq[UserId])

  case class RemoveUsersFromFriends(userId: UserId, friends: Seq[UserId])

  case class RemoveUserFromGroup(userId: UserId, friendId: UserId, groups: Seq[UserGroupType])

  case class AddUserToGroup(userId: UserId, friendId: UserId, groups: Seq[UserGroupType])

}

class FriendCacheDataActor extends Actor with ActorHelper with AppActors {

  import FriendCacheDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val friendsCacheDataService = BeanLookup[FriendsCacheDataService]

  def receive = {
    case Find(userId, friendsGroup) => Future {
      friendsCacheDataService.findFriendsInCache(userId, friendsGroup.name()).map(_.friends)
    } pipeTo sender
    case Save(userId, friendsGroup, friendsIds) => Future {
      friendsCacheDataService.save(FriendsCache(userId = userId, friendGroupId = Option(friendsGroup.name()), friends = friendsIds))
    } pipeTo sender
    case AddUsersToFriends(userId, friends) => Future(friendsCacheDataService.addUsersToFriends(userId, friends)).pipeTo(sender)
    case RemoveUsersFromFriends(userId, friends) => Future(friendsCacheDataService.removeUsersFromFriends(userId, friends)).pipeTo(sender)
    case RemoveUserFromGroup(userId, friendId, groups) => Future { friendsCacheDataService.removeUserFromGroup(userId, friendId, groups) }
    case AddUserToGroup(userId, friendId, groups) => Future { friendsCacheDataService.addUserToGroup(userId, friendId, groups) }
  }
}