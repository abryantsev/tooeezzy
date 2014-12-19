package com.tooe.core.usecase.event_type

import com.tooe.api.service.ExecutionContextProvider
import com.tooe.core.application.Actors
import com.tooe.core.domain.EventTypeId
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.EventTypeDataService
import com.tooe.core.usecase.AppActor
import scala.concurrent.Future

object EventTypeDataActor {
  final val Id = Actors.EventTypeDataActor

  case class GetEventType(id: EventTypeId)
  case class GetEventTypes(ids: Seq[EventTypeId])
}

class EventTypeDataActor extends AppActor with ExecutionContextProvider{

  import EventTypeDataActor._

  lazy val service = BeanLookup[EventTypeDataService]

  def receive = {
    case GetEventType(id) => Future {
      service.findOne(id) getOrElse (throw NotFoundException(s"EventType(id=$id) not found"))
    } pipeTo sender

    case GetEventTypes(ids) => Future { service.getEventTypes(ids)} pipeTo sender
  }
}
