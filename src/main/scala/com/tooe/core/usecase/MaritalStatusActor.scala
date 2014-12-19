package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain.{Gender, GenderType, MaritalStatusId}
import com.tooe.core.usecase.maritalstatus.MaritalStatusDataActor
import com.fasterxml.jackson.annotation.JsonProperty
import akka.pattern.{ask, pipe}
import com.tooe.core.util.Lang
import com.tooe.core.db.mongo.domain.MaritalStatus
import scala.concurrent.Future
import com.tooe.api.service.SuccessfulResponse

object MaritalStatusActor {
  final val Id = Actors.MaritalStatus

  case class StatusInfoByGender(id: MaritalStatusId, lang: Lang, gender: Gender)

  case class Find(ids: Set[MaritalStatusId], lang: Lang)

  case class GetMaritalStatusInfo(id: MaritalStatusId, lang: Lang)

  case class FindByGender(gender: Option[GenderType],lang: Lang)

}

class MaritalStatusActor extends AppActor {

  lazy val maritalStatusDataActor = lookup(MaritalStatusDataActor.Id)

  import MaritalStatusActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case StatusInfoByGender(id, lang, gender) =>
      getMaritalStatus(id) map (ms =>
        ms.map(MaritalStatusInfo(lang,_,Some(GenderType(gender))))) pipeTo sender
    case Find(ids, lang) => find(ids) map (ms =>
        ms.map(MaritalStatusInfo(lang,_))) pipeTo sender
    case GetMaritalStatusInfo(id, lang) => getMaritalStatus(id) map (ms =>
        ms.map(MaritalStatusInfo(lang,_))) pipeTo sender
    case FindByGender(gender, lang) => allStatuses.map(statuses =>
      MaritalStatusResponse(statuses
        .sortWith(_.name.localized(lang).getOrElse("") < _.name.localized(lang).getOrElse(""))
        .map(MaritalStatusInfo(lang, _, gender)))) pipeTo sender
  }

  def find(ids: Set[MaritalStatusId]): Future[Seq[MaritalStatus]] =
    (maritalStatusDataActor ? MaritalStatusDataActor.Find(ids)).mapTo[Seq[MaritalStatus]]

  def getMaritalStatus(id: MaritalStatusId): Future[Option[MaritalStatus]] =
    (maritalStatusDataActor ? MaritalStatusDataActor.GetMaritalStatus(id)).mapTo[Option[MaritalStatus]]

  def allStatuses =
      maritalStatusDataActor.ask(MaritalStatusDataActor.FindAll).mapTo[Seq[MaritalStatus]]
}

case class MaritalStatusInfo
(
  @JsonProperty("id") id: MaritalStatusId,
  @JsonProperty("name") name: String
  )

object MaritalStatusInfo {
  def apply(lang: Lang, ms: MaritalStatus): MaritalStatusInfo = MaritalStatusInfo(
    id = ms.id,
    name = ms.name.localized(lang).orElse(ms.femaleStatusName.localized(lang)).getOrElse("")
  )

  def apply(lang: Lang, ms: MaritalStatus, gender: Option[GenderType]): MaritalStatusInfo = {
    val name = if(gender == Some(GenderType.Female))
      ms.femaleStatusName.localized(lang)
    else
      ms.name.localized(lang)

    MaritalStatusInfo(
      id = ms.id,
      name = name.getOrElse("")
    )
  }
}

case class MaritalStatusResponse(maritalstatuses: Seq[MaritalStatusInfo]) extends SuccessfulResponse

case class GetMaritalStatusesParams(gender: Option[GenderType])