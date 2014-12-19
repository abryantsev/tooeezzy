package com.tooe.core.usecase.region

import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.application.{Actors, AppActors}
import akka.actor.Actor
import akka.pattern._
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.RegionDataService
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.StatisticFields
import com.tooe.core.domain.{RegionId, CountryId}
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.usecase.statistics.UpdateRegionOrCountryStatistic

object RegionDataActor {
  final val Id = Actors.RegionData

  case class GetRegion(regionId: RegionId)

  case class GetRegionsByCountry(countryId: CountryId)

  case class FindByStatistics(countryId: CountryId, statisticsFields: StatisticFields)

  case class UpdateStatistics(regionId: RegionId, statistics: UpdateRegionOrCountryStatistic)

  case class FindCapital(country: CountryId)

  case class GetRegionLocationCategories(region: RegionId)
}

class RegionDataActor extends Actor with AppActors with DefaultTimeout {
  import RegionDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[RegionDataService]

   def receive = {
     case GetRegion(regionId) => Future {
       Option(service.find(regionId)) getOrElse (throw NotFoundException(s"$regionId doesn't exist"))
     } pipeTo sender

     case GetRegionsByCountry(countryId) => Future{ service.findByCountryId(countryId) } pipeTo sender

     case FindByStatistics(countryId, statisticFields) => Future { service.findByStatistics(countryId, statisticFields) } pipeTo sender

     case UpdateStatistics(regionId, statistics) => Future { service.updateStatistic(regionId, statistics)  }

     case FindCapital(country) => Future {
       service.findCountryCapital(country)
     } pipeTo sender

     case GetRegionLocationCategories(region) => Future {
       service.getRegionLocationCategories(region)
     } pipeTo sender
   }
 }
