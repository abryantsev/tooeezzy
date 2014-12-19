package com.tooe.core.usecase.online_status


import akka.actor.Actor
import com.tooe.core.service.OnlineStatusDataService
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.application.Actors
import com.tooe.core.domain.OnlineStatusId
import com.tooe.core.util.ActorHelper
import scala.concurrent.Future
import akka.pattern.pipe

object OnlineStatusDataActor {
  final val Id = Actors.OnlineStatusData

  case class GetStatuses(statusIds: Seq[OnlineStatusId])
  case object GetAllStatuses
}

class OnlineStatusDataActor extends Actor with ActorHelper {

  lazy val service = BeanLookup[OnlineStatusDataService]

  import OnlineStatusDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case GetStatuses(statusIds) => Future {
      service.getStatuses(statusIds)
    } pipeTo sender
    case GetAllStatuses => Future(service.findAll).pipeTo(sender)
  }
}
