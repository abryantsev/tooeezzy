package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.util.Lang
import com.tooe.core.usecase.lifecycle_status.LifecycleStatusDataActor
import akka.pattern.{pipe, ask}
import com.tooe.core.db.mongo.domain.LifecycleStatus
import com.tooe.api.service.SuccessfulResponse


object LifecycleStatusActor {
  val Id = Actors.Lifecycle

  case class GetAllLifecycleStatuses(lang: Lang)

}

class LifecycleStatusActor extends AppActor {

  import LifecycleStatusActor._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val lifecycleStatusDataActor = lookup(LifecycleStatusDataActor.Id)

  def receive = {
    case GetAllLifecycleStatuses(lang) =>
      implicit val l = lang
      val result = for {
        statuses <- getAllLifecycleStatuses
        sorted = statuses.sortWith(_.name.localized.getOrElse("") < _.name.localized.getOrElse(""))
      } yield {
        GetAllLifecycleStatusesResponse(sorted.map(s => GetAllLifecycleStatusesResponseItem(s.id.id, s.name.localized.getOrElse(""))))
      }
      result.pipeTo(sender)
  }

  def getAllLifecycleStatuses =
    lifecycleStatusDataActor.ask(LifecycleStatusDataActor.FindAll).mapTo[Seq[LifecycleStatus]]

}

case class GetAllLifecycleStatusesResponse(lifecyclestatuses: Seq[GetAllLifecycleStatusesResponseItem]) extends SuccessfulResponse

case class GetAllLifecycleStatusesResponseItem(id: String, name: String)
