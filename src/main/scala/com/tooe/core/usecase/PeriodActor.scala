package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.usecase.period.PeriodDataActor
import com.tooe.core.util.Lang
import akka.pattern.{pipe, ask}
import com.tooe.api.service.SuccessfulResponse
import com.tooe.core.db.mongo.domain.Period


object PeriodActor {
  final val Id = Actors.Period

  case class GetAllPeriods(lang: Lang)

}

class PeriodActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import PeriodActor._


  lazy val periodDataActor = lookup(PeriodDataActor.Id)

  def receive = {
    case GetAllPeriods(lang) =>
      getAllPeriods.map(periods => GetAllPeriodsResponse(periods.map(GetAllPeriodsResponseItem(lang)))).pipeTo(sender)

  }

  def getAllPeriods =
    periodDataActor.ask(PeriodDataActor.FindAll).mapTo[Seq[Period]]

}

case class GetAllPeriodsResponse(periods: Seq[GetAllPeriodsResponseItem]) extends SuccessfulResponse

case class GetAllPeriodsResponseItem(id: String, name: String)

object GetAllPeriodsResponseItem {
  def apply(lang: Lang)(p: Period): GetAllPeriodsResponseItem = GetAllPeriodsResponseItem(p.id.id, p.name.localized(lang).getOrElse(""))
}
