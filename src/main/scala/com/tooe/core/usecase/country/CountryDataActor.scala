package com.tooe.core.usecase.country

import com.tooe.core.application.Actors
import akka.pattern._
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CountryDataService
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.{Country, StatisticFields}
import com.tooe.core.domain.{CountryId, CountryField}
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.usecase.AppActor
import com.tooe.core.usecase.statistics.UpdateRegionOrCountryStatistic

object CountryDataActor {
  final val Id = Actors.CountryData

  case class GetCountry(countryId: CountryId)
  case class SaveCountry(country: Country)
  case class FindAll(fields: Set[CountryField])
  case class GetCountryByPhone(phone: String, fields: Set[CountryField])
  case class GetActiveCountry(fields: Set[CountryField])
  case class FindByStatistics(statisticsFields: StatisticFields, fields: Set[CountryField])

  case class UpdateStatistics(countryId: CountryId, statistics: UpdateRegionOrCountryStatistic)

}

class CountryDataActor extends AppActor {

  import CountryDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[CountryDataService]

  def receive = {
    case GetCountry(countryId) => Future{
      service.find(countryId) getOrElse (throw NotFoundException(s"$countryId doesn't exist"))
    } pipeTo sender

    case SaveCountry(country) => Future { service.save(country) }

    case FindAll(fields) => Future { service.findAll(fields) } pipeTo sender

    case GetCountryByPhone(phone, fields) => Future { service.findByPhone(phone, fields) } pipeTo sender

    case GetActiveCountry(fields) => Future { service.findActive(fields) } pipeTo sender

    case FindByStatistics(statisticFields, fields) => Future { service.findByStatistics(statisticFields, fields) } pipeTo sender

    case UpdateStatistics(countryId, statistics) => Future { service.updateStatistic(countryId, statistics) }

  }
}