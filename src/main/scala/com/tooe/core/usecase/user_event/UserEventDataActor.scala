package com.tooe.core.usecase.user_event

import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.UserEventDataService
import scala.concurrent.Future
import com.tooe.core.domain._
import com.tooe.core.usecase._
import com.tooe.api.service.OffsetLimit
import com.tooe.core.db.mongo.query.SkipLimitSort
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.UserEvent
import com.tooe.core.domain.UserEventId

object UserEventDataActor {
  final val Id = Actors.UserEventData

  case class Save(entity: UserEvent)
  case class Get(id: UserEventId)
  case class SaveMany(entities: Seq[UserEvent])
  case class FindByUserId(userId: UserId, offsetLimit: OffsetLimit)
  case class FindByFriendshipRequestId(friendshipRequestId: FriendshipRequestId)
  case class UpdateStatus(id: UserEventId, status: UserEventStatus)
  case class Delete(id: UserEventId)
  case class DeleteByUserId(id: UserId)
  case class UnsetFriendshipRequest(id: UserEventId)
}

class UserEventDataActor extends AppActor {

  lazy val service = BeanLookup[UserEventDataService]

  import scala.concurrent.ExecutionContext.Implicits.global
  import UserEventDataActor._

  def receive = {
    case Save(entity) => Future { service.save(entity) } pipeTo sender
    case Get(id) => Future(service.findOne(id) getOrElse (throw NotFoundException("Not found: "+id))) pipeTo sender
    case SaveMany(entities) => Future { service.saveMany(entities) }
    case FindByUserId(userId, offsetLimit) => Future {
      service.find(userId, SkipLimitSort(offsetLimit).desc("t").asc("id"))
    } pipeTo sender

    case FindByFriendshipRequestId(friendshipRequestId) =>
      Future(service.find(friendshipRequestId)) pipeTo sender

    case UpdateStatus(id, status) => Future(service.updateStatus(id, status)) pipeTo sender
    case Delete(id) => Future(service.delete(id))
    case DeleteByUserId(id) => Future(service.delete(id))

    case UnsetFriendshipRequest(id) => Future(service.unsetFriendshipRequestId(id)) pipeTo sender
  }
}