package com.tooe.core.usecase.locationschain
import com.tooe.core.usecase.AppActor
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.LocationsChainStats
import com.tooe.core.domain.{CountryId, LocationsChainId}
import com.tooe.core.service.LocationsChainStatsDataService
import scala.concurrent.ExecutionContext.Implicits.global
import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup

object LocationsChainStatsDataActor {
  val Id = Actors.LocationsChainStatsData

  case class IncreaseStatsCountersOrCreate(stats: LocationsChainStats)
  case class GetChainStatsById(id: LocationsChainId)
  case class GetChainStatsInCountry(chain: LocationsChainId, country: CountryId)
}

class LocationsChainStatsDataActor extends AppActor{
  import LocationsChainStatsDataActor._

  lazy val service = BeanLookup[LocationsChainStatsDataService]

  def receive = {
    case GetChainStatsById(id) => Future {service.findByChain(id)} pipeTo sender
    case IncreaseStatsCountersOrCreate(stats) => Future {
      locationsChainDataActor ! LocationsChainDataActor.IncreaseLocationsCounter(stats.chainId, stats.locationsCount)
      service.mergeStatsAndSave(stats)
    } pipeTo sender
    case GetChainStatsInCountry(chain, country) => Future {
      service.findByChainAndCountry(chain, country)
    } pipeTo sender
  }

  lazy val locationsChainDataActor = lookup(LocationsChainDataActor.Id)
}