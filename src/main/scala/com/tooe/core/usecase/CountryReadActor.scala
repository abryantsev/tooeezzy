package com.tooe.core.usecase

import com.tooe.core.db.mongo.domain._
import com.tooe.core.application.Actors
import com.tooe.core.usecase.country.CountryDataActor
import com.tooe.api.service.{SuccessfulResponse, RouteContext}
import com.tooe.core.domain.{CountryField, CountryId}
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.util.Lang

object CountryReadActor {
  final val Id = Actors.CountryRead

  case class GetCountries(fields: Set[CountryField], ctx: RouteContext)
  case class GetCountryByPhone(phone: String, fields: Set[CountryField], ctx: RouteContext)
  case class GetActiveCountry(fields: Set[CountryField], ctx: RouteContext)
  case class FindByStatistics(statisticsFields: StatisticFields, fields: Set[CountryField], ctx: RouteContext)
  case class GetCountryItem(id: CountryId, lang: Lang)
}

class CountryReadActor extends AppActor {

  lazy val countryDataActor = lookup(CountryDataActor.Id)

  import CountryReadActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case GetCountries(fields, ctx) =>
      implicit val lang = ctx.lang
      (countryDataActor ? CountryDataActor.FindAll(fields)).mapTo[List[Country]] map Countries.apply pipeTo sender

    case GetCountryByPhone(phone, fields, ctx) =>
      implicit val lang = ctx.lang
      (countryDataActor ? CountryDataActor.GetCountryByPhone(phone, fields)).mapTo[List[Country]] map Countries.apply pipeTo sender

    case GetActiveCountry(fields, ctx) =>
      implicit val lang = ctx.lang
      (countryDataActor ? CountryDataActor.GetActiveCountry(fields)).mapTo[List[Country]] map Countries.apply pipeTo sender

    case FindByStatistics(statisticFields, fields, ctx) =>
      implicit val lang = ctx.lang
      (countryDataActor ? CountryDataActor.FindByStatistics(statisticFields, fields)).mapTo[List[Country]] map Countries.apply pipeTo sender

    case GetCountryItem(id, lang) =>
      (countryDataActor ? CountryDataActor.GetCountry(id)).mapTo[Country] map CountryDetailsItem(lang) pipeTo sender
  }
}

case class Countries(countries: Seq[CountryItem]) extends SuccessfulResponse

object Countries {
  def apply(countries: Seq[Country])(implicit lang: Lang): Countries =
    Countries(countries.sortBy(_.name.localized.getOrElse("")).map(CountryItem.apply))
}

case class CountryItem(id: String, name: Option[String], @JsonProperty("phone") phoneCode: Option[String])

object CountryItem {
  def apply(country: Country)(implicit lang: Lang): CountryItem =
    CountryItem(country.id.id, country.name.localized, Option(country.phoneCode))
}

/**
 * TODO reuse CountryItem. The only difference is code/phone field that are the same and differ only by name
 */
case class CountryDetailsItem(id: CountryId, name: String, code: String)

object CountryDetailsItem {
  def apply(lang: Lang)(country: Country): CountryDetailsItem = CountryDetailsItem(
    id = country.id,
    name = country.name.localized(lang) getOrElse "",
    code = country.phoneCode
  )
}