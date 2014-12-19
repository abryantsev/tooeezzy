package com.tooe.core.usecase.event_group

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.EventGroupDataService
import scala.concurrent.Future

object EventGroupDataActor {
  final val Id = Actors.EventGroupData

  case object FindAll

}

class EventGroupDataActor extends AppActor {

  import EventGroupDataActor._

  implicit val ec = scala.concurrent.ExecutionContext.global
  lazy val service = BeanLookup[EventGroupDataService]

  def receive = {
    case FindAll => Future(service.findAll).pipeTo(sender)
  }

}
