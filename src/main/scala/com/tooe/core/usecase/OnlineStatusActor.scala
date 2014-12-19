package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import com.tooe.core.usecase.online_status.OnlineStatusDataActor
import akka.pattern.{ask, pipe}
import com.tooe.core.db.mongo.domain.OnlineStatus
import com.tooe.api.service.SuccessfulResponse

object OnlineStatusActor {
  final val Id = Actors.OnlineStatus

  case class GetAllOnlineStatuses(lang: Lang)

}

class OnlineStatusActor extends AppActor {

  import OnlineStatusActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val onlineStatusDataActor = lookup(OnlineStatusDataActor.Id)

  def receive = {
    case GetAllOnlineStatuses(lang) =>
      getAllOnlineStatuses.map(statuses =>
        GetAllOnlineStatusesResponse(statuses.map(GetAllOnlineStatusesResponseItem(lang)))).pipeTo(sender)
  }

  def getAllOnlineStatuses =
    onlineStatusDataActor.ask(OnlineStatusDataActor.GetAllStatuses).mapTo[Seq[OnlineStatus]]

}

case class GetAllOnlineStatusesResponse(onlinestatuses: Seq[GetAllOnlineStatusesResponseItem]) extends SuccessfulResponse

case class GetAllOnlineStatusesResponseItem(id: String, name: String)

object GetAllOnlineStatusesResponseItem {
  def apply(lang: Lang)(os: OnlineStatus): GetAllOnlineStatusesResponseItem =
    GetAllOnlineStatusesResponseItem(os.id.id, os.name.localized(lang).getOrElse(""))
}
