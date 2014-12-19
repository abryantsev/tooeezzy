package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.usecase.moderation_status.ModerationStatusDataActor
import akka.pattern.{pipe, ask}
import com.tooe.core.db.mongo.domain.ModerationStatus

object ModerationStatusActor {
  final val Id = Actors.ModerationStatus

  case class GetAllModerationStatues(lang: Lang)

}

class ModerationStatusActor extends AppActor {

  import ModerationStatusActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val moderationStatusDataActor = lookup(ModerationStatusDataActor.Id)

  def receive = {
    case GetAllModerationStatues(lang) =>
      getAllModerationStatuses.map(statuses =>
        GetAllModerationStatuesResponse(statuses
          .sortWith(_.name.localized(lang).getOrElse("") < _.name.localized(lang).getOrElse(""))
          .map(GetAllModerationStatuesResponseItem(lang)))).pipeTo(sender)
  }

  def getAllModerationStatuses =
    moderationStatusDataActor.ask(ModerationStatusDataActor.FindAll).mapTo[Seq[ModerationStatus]]
}

case class GetAllModerationStatuesResponse(moderationstatuses: Seq[GetAllModerationStatuesResponseItem]) extends SuccessfulResponse

case class GetAllModerationStatuesResponseItem(id: String, name: String, description: String)

object GetAllModerationStatuesResponseItem {
  def apply(lang: Lang)(ms: ModerationStatus): GetAllModerationStatuesResponseItem =
    GetAllModerationStatuesResponseItem(
      ms.id.id,
      ms.name.localized(lang).getOrElse(""),
      ms.description.localized(lang).getOrElse(""))
}