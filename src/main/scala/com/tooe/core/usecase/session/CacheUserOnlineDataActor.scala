package com.tooe.core.usecase.session

import akka.actor.Actor
import com.tooe.core.util.ActorHelper
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CacheUserOnlineDataService
import com.tooe.core.application.Actors
import com.tooe.core.domain.{OnlineStatusId, UserId}
import scala.concurrent.Future
import akka.pattern.pipe
import java.util.Date
import com.tooe.api.service.OffsetLimit

object CacheUserOnlineDataActor {
  final val Id = Actors.CacheUserOnlineData

  case class Upsert(id: UserId, createdAt: Date, onlineStatusId: OnlineStatusId)

  case class UpdateOnlineStatus(userId: UserId, status: OnlineStatusId)

  case class UpdateFriends(userId: UserId, friends: Seq[UserId])
  case class AddFriend(userId: UserId, friend: UserId)
  case class RemoveFriend(userId: UserId, friend: UserId)

  case class Delete(userId: UserId)
  case class GetUsersStatuses(userIds: Seq[UserId])
  case class FindOnlineUsers(userIds: Seq[UserId])
  case class GetOnlineFriends(userId: UserId, offsetLimit: OffsetLimit)
  case class CountOnlineFriends(userId: UserId)

  case class FindCacheUserOnline(userId: UserId)
}

class CacheUserOnlineDataActor extends Actor with ActorHelper {

  lazy val service = BeanLookup[CacheUserOnlineDataService]

  import CacheUserOnlineDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def receive = {
    case Upsert(id, createdAt, onlineStatusId) => Future(service.upsert(id, createdAt, onlineStatusId)) pipeTo sender

    case UpdateOnlineStatus(userId, status) => Future(service.updateOnlineStatus(userId, status))

    case UpdateFriends(userId, friends) => Future(service.updateFriends(userId, friends))
    case AddFriend(userId, friend) => Future(service.addFriend(userId, friend))
    case RemoveFriend(userId, friend) => Future(service.removeFriend(userId, friend))

    case GetUsersStatuses(userIds) => Future {
      service.getUsersStatuses(userIds)
    } pipeTo sender

    case FindOnlineUsers(userIds) => Future {
      service.findOnlineUsers(userIds)
    } pipeTo sender

    case Delete(userId) => Future {
      service.delete(userId)
    }
    case GetOnlineFriends(userId, offsetLimit) => Future { service.getOnlineFriends(userId, offsetLimit) } pipeTo sender
    case CountOnlineFriends(userId) => Future { service.countOnlineFriends(userId) } pipeTo sender

    case FindCacheUserOnline(userId) => Future(service.find(userId)) pipeTo sender
  }
}