package com.tooe.core.usecase

import com.tooe.core.usecase.location.{ModerationLocationSearchSortType, PreModerationLocationAdminSearchSortType, PreModerationLocationDataActor}
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain.{Company, LocationsChain, Region, PreModerationLocation}
import com.tooe.api.service.{OffsetLimit, SuccessfulResponse}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import org.bson.types.ObjectId
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.PreModerationLocationId
import com.tooe.core.domain.LocationDetails
import com.tooe.core.util.{Images, Lang}
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.application.Actors
import com.tooe.core.service.PreModerationLocationAdminSearchParams
import com.tooe.core.usecase.locationschain.LocationsChainDataActor
import com.tooe.core.usecase.company.CompanyDataActor

object PreModerationLocationReadActor {
  final val Id = Actors.PreModerationLocationRead

  case class GetPreModerationLocationByLocationId(locationId: LocationId, lang: Lang)

  case class GetPreModerationLocationById(id: PreModerationLocationId, lang: Lang)

  case class SearchPreModerationLocations(request: SearchPreModerationLocationsRequest, offsetLimit: OffsetLimit, lang: Lang)

  case class ModerationLocationsSearch(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang)

}

class PreModerationLocationReadActor extends AppActor {

  import PreModerationLocationReadActor._

  implicit val ec = scala.concurrent.ExecutionContext.global

  lazy val preModerationLocationDataActor = lookup(PreModerationLocationDataActor.Id)
  lazy val locationsChainDataActor = lookup(LocationsChainDataActor.Id)
  lazy val companyDataActor = lookup(CompanyDataActor.Id)
  lazy val regionDataActor = lookup(RegionDataActor.Id)

  def receive = {
    case ModerationLocationsSearch(request, companies, lang) =>
      implicit val l = lang
      val result = for {
        locations <- moderationLocationsSearch(request, companies, lang)
        count <- countModerationLocationsSearch(request, companies, lang)
        locationChains <- getLocationsChainsByIds(locations.flatMap(_.locationsChainId))
        locationChainsById = locationChains.toMapId(_.id)
      } yield {
        val items = locations.map(ModerationLocationsSearchResponseItem(locationChainsById))
        ModerationLocationsSearchResponse(count, items)
      }
      result pipeTo sender
    case GetPreModerationLocationById(id, lang) =>
      implicit val l = lang
      val result = for {
        location <- getPreModerationLocationById(id)
        region <- getRegion(location.contact.address.regionId)
      } yield GetPreModerationLocationResponse(location, region)
      result.pipeTo(sender)
    case GetPreModerationLocationByLocationId(locationId, lang) =>
      implicit val l = lang
      val result = for {
        location <- getPreModerationLocationByLocationId(locationId)
        region <- getRegion(location.contact.address.regionId)
      } yield GetPreModerationLocationResponse(location, region)
      result.pipeTo(sender)
    case SearchPreModerationLocations(request, offset, lang) =>
      implicit val l = lang
      val params = PreModerationLocationAdminSearchParams(request, offset, lang)
      val result = for {
        locations <- findPreModerationLocationByAdminSearchParams(params)
        count <- countPreModerationLocationByAdminSearchParams(params)
        locationChains <- getLocationsChainsByIds(locations.flatMap(_.locationsChainId))
        companies <- getCompaniesByIds(locations.map(_.companyId))
        companiesById = companies.toMapId(_.id)
        locationChainsById = locationChains.toMapId(_.id)
      } yield {
        val items = locations.map(SearchPreModerationLocationsResponseItem(companiesById, locationChainsById))
        SearchPreModerationLocationsResponse(count, items)
      }
      result.pipeTo(sender)

  }

  def getPreModerationLocationById(id: PreModerationLocationId) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.FindLocationById(id)).mapTo[PreModerationLocation]

  def getPreModerationLocationByLocationId(locationId: LocationId) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.FindLocationByLocationId(locationId)).mapTo[PreModerationLocation]

  def findPreModerationLocationByAdminSearchParams(params: PreModerationLocationAdminSearchParams) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.FindLocationsByAdminSearchParams(params)).mapTo[Seq[PreModerationLocation]]

  def countPreModerationLocationByAdminSearchParams(params: PreModerationLocationAdminSearchParams) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.CountLocationsByAdminSearchParams(params)).mapTo[Long]

  def moderationLocationsSearch(request: ModerationLocationsSearchRequest, companies: Set[CompanyId],  lang: Lang) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.ModerationLocationSearch(request, companies, lang)).mapTo[Seq[PreModerationLocation]]

  def countModerationLocationsSearch(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang) =
    preModerationLocationDataActor.ask(PreModerationLocationDataActor.CountModerationLocationSearch(request, companies, lang)).mapTo[Long]

  def getRegion(regionId: RegionId) =
    regionDataActor.ask(RegionDataActor.GetRegion(regionId)).mapTo[Region]

  def getLocationsChainsByIds(ids: Seq[LocationsChainId]) =
    locationsChainDataActor.ask(LocationsChainDataActor.FindLocationsChains(ids)).mapTo[Seq[LocationsChain]]

  def getCompaniesByIds(ids: Seq[CompanyId]) =
    companyDataActor.ask(CompanyDataActor.GetCompanies(ids)).mapTo[Seq[Company]]

}

case class SearchPreModerationLocationsResponse(@JsonProperty("locationscount") count: Long, locations: Seq[SearchPreModerationLocationsResponseItem]) extends SuccessfulResponse

case class SearchPreModerationLocationsResponseItemCompany(companyid: ObjectId, name: String) extends UnmarshallerEntity

object SearchPreModerationLocationsResponseItemCompany {

  def apply(c: Company) : SearchPreModerationLocationsResponseItemCompany =
    SearchPreModerationLocationsResponseItemCompany(c.id.id, c.name)

}

case class SearchPreModerationLocationsResponseItemLocationChain(locationschainid: ObjectId, name: String) extends UnmarshallerEntity

object SearchPreModerationLocationsResponseItemLocationChain {
  def apply(lc: LocationsChain)(implicit l: Lang) : SearchPreModerationLocationsResponseItemLocationChain =
    SearchPreModerationLocationsResponseItemLocationChain(lc.id.id, lc.name.localized.getOrElse(""))
}

case class SearchPreModerationLocationsResponseItem
(
  id: ObjectId,
  @JsonProperty("locationid") locationId: Option[ObjectId],
  name: String,
  company: SearchPreModerationLocationsResponseItemCompany,
  address: LocationAddressItem,
  @JsonProperty("locationschain") locationsChain: Option[SearchPreModerationLocationsResponseItemLocationChain],
  media: Option[MediaShortItem],
  @JsonProperty("mainphone") mainPhone: Option[LocationPhone],
  moderation: ModerationStatusItem
  ) extends UnmarshallerEntity

object SearchPreModerationLocationsResponseItem {

  def apply(companies: Map[CompanyId, Company], chains: Map[LocationsChainId, LocationsChain])(location: PreModerationLocation)(implicit l: Lang): SearchPreModerationLocationsResponseItem =
    SearchPreModerationLocationsResponseItem(
      id = location.id.id,
      locationId = location.publishedLocation.map(_.id),
      name = location.name.localized.getOrElse(""),
      company = SearchPreModerationLocationsResponseItemCompany(companies(location.companyId)),
      address = LocationAddressItem(location.contact.address),
      locationsChain = location.locationsChainId.map(chains).map(SearchPreModerationLocationsResponseItemLocationChain(_)),
      media = location.locationMedia.find(_.purpose == Some("main")).map(MediaShortItem(_, Images.PreModerationLocationSearch.Full.Self.Media)),
      mainPhone = location.contact.phones.find(_.purpose == Some("main")).map(LocationPhone(_)),
      moderation = ModerationStatusItem(location.moderationStatus)
    )
}


case class SearchPreModerationLocationsRequest
(
  name: Option[String],
  company: Option[CompanyId],
  modstatus: Option[ModerationStatusId],
  sort: Option[PreModerationLocationAdminSearchSortType]
  )

case class GetPreModerationLocationResponse(location: GetPreModerationLocationResponseItem) extends SuccessfulResponse

object GetPreModerationLocationResponse {
  def apply(plm: PreModerationLocation, region: Region)(implicit lang: Lang): GetPreModerationLocationResponse =
    GetPreModerationLocationResponse(GetPreModerationLocationResponseItem(
      id = plm.id.id,
      name = plm.name.localized.getOrElse(""),
      openingHours = plm.openingHours.localized.getOrElse(""),
      details = LocationDetails(plm.description.localized.getOrElse("")),
      coords = plm.contact.address.coordinates,
      address = LocationFullAddressItem(plm.contact.address, region),
      media = plm.locationMedia.find(_.purpose == Some("main")).map(MediaShortItem(_, Images.PreModerationLocation.Full.Self.Media)),
      mainPhone = plm.contact.phones.find(_.purpose == Some("main")).map(LocationPhone(_)),
      categories = plm.locationCategories.map(_.id),
      moderation = ModerationStatusItem(plm.moderationStatus)
    ))
}

case class GetPreModerationLocationResponseItem
(
  id: ObjectId,
  name: String,
  @JsonProperty("openinghours") openingHours: String,
  details: LocationDetails,
  coords: Coordinates,
  address: LocationFullAddressItem,
  media: Option[MediaShortItem],
  @JsonProperty("mainphone") mainPhone: Option[LocationPhone],
  categories: Seq[String],
  moderation: ModerationStatusItem
  ) extends UnmarshallerEntity

case class ModerationLocationsSearchRequest
(
  name: Option[String],
  @JsonProperty("modstatus") moderationStatus: Option[ModerationStatusId],
  sort: Option[ModerationLocationSearchSortType],
  offsetLimit: OffsetLimit
) {
  def sortField = sort.getOrElse(ModerationLocationSearchSortType.Name)
}

case class ModerationLocationsSearchResponse(@JsonProperty("locationscount") count: Long, locations: Seq[ModerationLocationsSearchResponseItem]) extends SuccessfulResponse

case class ModerationLocationsSearchResponseItem
(
  id: PreModerationLocationId,
  @JsonProperty("locationid") locationId: Option[LocationId],
  name: String,
  address: LocationAddressItem,
  @JsonProperty("locationschain") locationsChain: Option[SearchPreModerationLocationsResponseItemLocationChain],
  media: Option[MediaShortItem],
  @JsonProperty("mainphone") mainPhone: Option[LocationPhone],
  moderation: ModerationStatusItem
  ) extends UnmarshallerEntity

object ModerationLocationsSearchResponseItem {

  def apply(chains: Map[LocationsChainId, LocationsChain])(location: PreModerationLocation)(implicit l: Lang): ModerationLocationsSearchResponseItem =
    ModerationLocationsSearchResponseItem(
      id = location.id,
      locationId = location.publishedLocation,
      name = location.name.localized.getOrElse(""),
      address = LocationAddressItem(location.contact.address),
      locationsChain = location.locationsChainId.map(chains).map(SearchPreModerationLocationsResponseItemLocationChain(_)),
      media = location.locationMedia.find(_.purpose == Some("main")).map(MediaShortItem(_, Images.PreModerationLocationSearch.Full.Self.Media)),
      mainPhone = location.contact.phones.find(_.purpose == Some("main")).map(LocationPhone(_)),
      moderation = ModerationStatusItem(location.moderationStatus)
    )
}
