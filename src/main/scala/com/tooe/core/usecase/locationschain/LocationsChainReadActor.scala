package com.tooe.core.usecase.locationschain

import com.tooe.core.usecase.AppActor
import com.tooe.core.application.Actors
import com.tooe.core.domain._
import com.tooe.api.service.{SuccessfulResponse, ExecutionContextProvider}
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.core.util.{Images, Lang}
import com.tooe.core.domain.MediaUrl
import com.tooe.core.domain.CompanyId
import com.tooe.api.service.RouteContext
import com.tooe.core.db.mongo.domain.{Region, LocationsChainStats, LocationsChain}
import com.tooe.core.domain.CountryId
import com.tooe.core.domain.LocationsChainId
import com.tooe.core.usecase.locationschain.LocationsChainDataActor.FindLocationsChain
import org.bson.types.ObjectId
import com.tooe.core.usecase.region.RegionDataActor
import scala.concurrent.Future
import com.tooe.core.util.MediaHelper._

object LocationsChainReadActor {
  final val Id = Actors.LocationsChainRead

  case class GetLocationChain(id: LocationsChainId, routeContext: RouteContext)
  case class GetLocationsChainStats(id: LocationsChainId)
  case class GetChainRegions(chain: LocationsChainId, country: CountryId, routeContext: RouteContext)
}

class LocationsChainReadActor extends AppActor with ExecutionContextProvider {

  import LocationsChainReadActor._

  lazy val locationsChainStatsDataActor = lookup(LocationsChainStatsDataActor.Id)
  lazy val locationsChainDataActor = lookup(LocationsChainDataActor.Id)
  lazy val regionDataActor = lookup(RegionDataActor.Id)

  def receive = {
    case GetLocationChain(id, rc) =>
      (locationsChainDataActor ? FindLocationsChain(id)).mapTo[LocationsChain].map {
        locationsChain =>
          implicit val lang = rc.lang
          GetLocationsChainResponse(LocationsChainItem(locationsChain))
      } pipeTo sender
    case GetLocationsChainStats(id) =>
      (locationsChainStatsDataActor ? LocationsChainStatsDataActor.GetChainStatsById(id)).mapTo[Seq[LocationsChainStats]].map {
        stats =>
          LocationsChainStatsResponse(LocationsChainStatsCountries(stats.map(LocationsChainStatsPerCountry(_))))
      } pipeTo sender
    case GetChainRegions(chain, country, rc) =>
      val response = (locationsChainStatsDataActor ? LocationsChainStatsDataActor.GetChainStatsInCountry(chain, country)).mapTo[LocationsChainStats].flatMap {
        stats =>
          Future.traverse(stats.regions)(regionStats => (regionDataActor ? RegionDataActor.GetRegion(regionStats.region)).mapTo[Region].map {
            region =>
              LocationsChainRegionStatistics(region.id.id, region.name.localized(rc.lang).getOrElse(""))
          })
      }.map(LocationsChainRegionsResponse)
      response pipeTo sender
  }
}

case class GetLocationsChainResponse(@JsonProperty("locationschain") locationsChain: LocationsChainItem) extends SuccessfulResponse

case class LocationsChainItem(id: LocationsChainId,
                              @JsonProperty("companyid") companyId: CompanyId,
                              name: String,
                              description: String,
                              time: Date,
                              media: MediaUrl,
                              statistics: LocationsChainItemStatistics)

object LocationsChainItem {

  def apply(locationsChain: LocationsChain)(implicit lang: Lang): LocationsChainItem =
    LocationsChainItem(
      id = locationsChain.id,
      companyId = locationsChain.companyId,
      name = locationsChain.name.localized.getOrElse(""),
      description = locationsChain.description.flatMap(_.localized).getOrElse(""),
      time = locationsChain.registrationDate,
      media = locationsChain.locationChainMedia.headOption.map(_.media).asMediaUrl(Images.Locationschain.Full.Self.Media, LocationDefaultUrlType),
      statistics = LocationsChainItemStatistics(locationsChain.locationCount)
    )

}

case class LocationsChainItemStatistics(@JsonProperty("locationscount") locationCount: Int)

case class LocationsChainStatsResponse(statistics: LocationsChainStatsCountries) extends SuccessfulResponse
case class LocationsChainStatsCountries(countries: Seq[LocationsChainStatsPerCountry])
case class LocationsChainStatsPerCountry(@JsonProperty("countryid") countryId: CountryId, @JsonProperty("locationscount") locationsCount: Int, coords: Coordinates)
object LocationsChainStatsPerCountry {
  def apply(stats: LocationsChainStats): LocationsChainStatsPerCountry = {
    LocationsChainStatsPerCountry(stats.countryId, stats.locationsCount, stats.coordinates)
  }
}

case class LocationsChainRegionsResponse(regions: Seq[LocationsChainRegionStatistics]) extends SuccessfulResponse
case class LocationsChainRegionStatistics(id: ObjectId, name: String)