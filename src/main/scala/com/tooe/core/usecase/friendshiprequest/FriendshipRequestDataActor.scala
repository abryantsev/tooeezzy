package com.tooe.core.usecase.friendshiprequest

import com.tooe.core.application.Actors
import com.tooe.core.usecase.AppActor
import com.tooe.core.service.FriendshipRequestDataService
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.db.mongo.domain.FriendshipRequest
import com.tooe.core.domain.{UserId, FriendshipRequestId}
import scala.concurrent.Future
import akka.pattern.pipe
import com.tooe.api.service.OffsetLimit
import com.tooe.core.exceptions.NotFoundException

object FriendshipRequestDataActor {
  final val Id = Actors.FriendshipRequestData

  case class Save(entity: FriendshipRequest)
  case class Find(id: FriendshipRequestId)
  case class Get(id: FriendshipRequestId)
  case class Delete(id: FriendshipRequestId)
  case class FindByUser(userId: UserId, offsetLimit: OffsetLimit)
  case class FindRequest(userId: UserId, actorId: UserId)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
}

class FriendshipRequestDataActor extends AppActor {

  lazy val service = BeanLookup[FriendshipRequestDataService]

  import FriendshipRequestDataActor._

  def receive = {
    case Save(entity) => Future(service.save(entity)) pipeTo sender
    case Find(id) => Future(service.findOne(id)) pipeTo sender
    case Get(id) => Future(service.findOne(id) getOrElse (throw NotFoundException("Not found friendship request "+id))) pipeTo sender
    case Delete(id) => Future(service.delete(id))
    case FindByUser(userId, offsetLimit) => Future(service.findByUser(userId, offsetLimit)) pipeTo sender
    case FindRequest(userId, actorId) => Future(service.find(userId = userId, actorId = actorId)) pipeTo sender
  }
}
