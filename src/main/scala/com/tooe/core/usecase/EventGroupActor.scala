package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import com.tooe.core.usecase.event_group.EventGroupDataActor
import com.tooe.core.db.mongo.domain.EventGroup
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.db.mongo.util.UnmarshallerEntity

object EventGroupActor {

  final val Id = Actors.EventGroup

  case class GetAllEventGroups(lang: Lang)

}

class EventGroupActor extends AppActor {

  import EventGroupActor._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val eventGroupDataActor = lookup(EventGroupDataActor.Id)

  def receive = {
    case GetAllEventGroups(lang) =>
      getAllEventGroups.map(groups =>
        GetAllEventGroupsResponse(groups.map(GetAllEventGroupsResponseItem(lang)).sortBy(_.name)))
        .pipeTo(sender)
  }

  def getAllEventGroups =
    eventGroupDataActor.ask(EventGroupDataActor.FindAll).mapTo[Seq[EventGroup]]

}

case class GetAllEventGroupsResponse(eventgroups: Seq[GetAllEventGroupsResponseItem]) extends SuccessfulResponse

case class GetAllEventGroupsResponseItem(id: String, name: String) extends UnmarshallerEntity

object GetAllEventGroupsResponseItem {

  def apply(l: Lang)(eg: EventGroup): GetAllEventGroupsResponseItem =
    GetAllEventGroupsResponseItem(eg.id.id, eg.name.localized(l).getOrElse(""))

}
