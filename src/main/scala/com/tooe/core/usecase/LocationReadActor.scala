package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.usecase.location.{LocationDataActor, LocationSearchViewType, LocationSearchSortType}
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.graph._
import com.tooe.core.db.graph.msg._
import com.tooe.core.usecase.location_category.{LocationCategoryItem, LocationCategoryActor}
import location_photo.LocationPhotoDataActor
import scala.collection.JavaConverters._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.service._
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.api._
import com.tooe.core.domain._
import com.tooe.core.util.{Images, Lang}
import com.tooe.core.util.MediaHelper._
import scala.concurrent.Future
import service.OffsetLimit
import com.tooe.api.validation.ValidationHelper
import validation.{ValidationContext, Validatable}
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor
import com.tooe.core.usecase.region.RegionDataActor
import org.springframework.data.mongodb.core.geo.Point
import com.tooe.core.main.SharedActorSystem
import com.tooe.extensions.scala.Settings
import com.javadocmd.simplelatlng.{LatLngTool, LatLng}
import com.javadocmd.simplelatlng.util.LengthUnit
import scala.Some
import com.tooe.api.service.RouteContext
import com.tooe.core.usecase.location_photo.LocationPhotoDataActor.GetLocationPhotos
import com.tooe.core.exceptions.ApplicationException
import Implicits._
import akka.actor.ActorContext

object LocationReadActor {
  final val Id = Actors.LocationRead

  case class GetFavoriteLocations(request: GetFavoriteLocationsRequest, lang: Lang)
  case class GetProductCategoriesOfLocation(locationId: LocationId, ctx: RouteContext)
  case class GetLocationMoreInfo(locationId: LocationId, ctx: RouteContext)
  case class GetLocationInfo(locationId: LocationId, userId: UserId, viewType: ShowType, usersLimit: Int, ctx: RouteContext)
  case class GetLocationAdminInfo(locationId: LocationId, userId: AdminUserId, viewType: ShowType, usersLimit: Int, ctx: RouteContext)
  case class FindUserEventLocations(ids: Set[LocationId], lang: Lang, imageDimension: String)
  case class GetLocationsSearch(request: LocationsSearchRequest, lang: Lang)
  case class GetLocations(ids: Seq[LocationId], lang: Lang)
  case class GetProductMoreInfoResponseLocation(id: LocationId, lang: Lang)
  case class GetProductLocationItem(id: LocationId, lang: Lang)
  case class GetPromotionLocationFullItem(id: LocationId, lang: Lang)
  case class GetLocationItem(id: LocationId, lang: Lang)
  case class GetLocationStatistics(locationId: LocationId)
  case class GetLocationsForInvitation(request: GetLocationsForInvitationRequest, currentUserId: UserId, lang: Lang)
  case class GetLocationsByChain(request: GetLocationsByChainRequest, lang: Lang)
  case class GetLocationsForCheckin(request: GetLocationsForCheckinRequest, lang: Lang)
}

class LocationReadActor extends AppActor {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationReadActor._

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val locationCategoryActor = lookup(LocationCategoryActor.Id)
  lazy val getFavoriteLocationsGraphActor = lookup(GraphGetFavoritesActor.Id)
  lazy val checkinReadActor = lookup(CheckinReadActor.Id)
  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val graphGetFavoritesActor = lookup(GraphGetFavoritesActor.Id)
  lazy val locationSubscriptionDataActor = lookup(LocationSubscriptionDataActor.Id)
  lazy val regionDataActor = lookup(RegionDataActor.Id)

  def receive = {
    case GetLocationsForCheckin(request, lang) =>
      implicit val lng = lang
      (for {
        locationsResult <- locationDataActor.ask(LocationDataActor.GetLocationsForCheckin(request, lang)).mapTo[LocationsWithCountResult]
        locationCategoryMap <- getLocationCategoryItemMap(locationsResult.locations)
      } yield GetLocationsForCheckinResponse(
          locationsResult.locations.map(l => GetLocationsForCheckinResponseLocationItem(l, locationCategoryMap, request.latLng)),
          locationsResult.locationsCount 
      ))  pipeTo sender

    case GetLocationsByChain(request, lang) =>
      implicit val lng = lang
      (for {
        locationsResult <- locationDataActor.ask(LocationDataActor.GetLocationsByChain(request, lang)).mapTo[LocationsWithCountResult]
        locationCategoryMap <- getLocationCategoryItemMap(locationsResult.locations)
      } yield GetLocationsByChainResponse(
          locationsResult.locations.map(l => GetLocationsByChainResponseLocationItem(l, locationCategoryMap)),
          locationsResult.locationsCount
      ))  pipeTo sender
      
    case GetLocationsForInvitation(request, currentUserId, lang) =>
      implicit val lng = lang
      def validate = Future{
        ValidationHelper.check(_.require(
          if(request.userIds.isDefined && request.userIds.get.size == 2) request.userIds.get.contains(currentUserId) else true, "One of userIds must be from current user")
        )
      }
      (for {
        _ <- validate
        locationsResult <- locationDataActor.ask(LocationDataActor.GetLocationsForInvitations(request, lang)).mapTo[LocationsWithCountResult]
      } yield GetLocationsForInvitationResponse(
          locationsResult.locations.map(l => GetLocationsForInvitationLocationItem(l)),
          locationsResult.locationsCount 
      ))  pipeTo sender
    case GetLocationInfo(locationId, userId, viewType, usersLimit, ctx) =>
      implicit val lang = ctx.lang
      (for {
        location <- getLocation(locationId)
        locationPhotos <- getLocationPhotos(location, viewType)
        isFavorite <- getIsFavorite(userId, location, viewType)
        locationCheckinInfoItem <- getLocationCheckinInfo(locationId, userId, viewType)
        selfSubscribed <- getSelfSubscribed(userId, location.id, viewType)
      } yield viewType match {
        case ShowType.Short => LocationInfo(LocationInfoItem(location, isFavorite, locationPhotos, locationCheckinInfoItem))
        case ShowType.None => LocationFullInfo(LocationInfoFullItem(location, isFavorite, locationPhotos, selfSubscribed))
        case ShowType.Mini => LocationMiniInfo(LocationInfoMiniItem(location, locationPhotos))
        case _ => throw new ApplicationException(message = "User can see short, full or mini format")
      } ) pipeTo sender

    case GetLocationAdminInfo(locationId, userId, viewType, usersLimit, ctx) =>
      implicit val lang = ctx.lang
      (for {
        location <- getLocationForAdmin(locationId)
        region <- (regionDataActor ? RegionDataActor.GetRegion(location.contact.address.regionId)).mapTo[Region]
      } yield LocationAdminInfo(LocationInfoAdminItem(location, region))) pipeTo sender


    case GetLocationsSearch(request, lang) =>
      implicit val language = lang

      val result = for {
        locationSearchResult <- getLocationsSearchByCoordinates(request, lang)
        locationCategoryMap <- getLocationCategoryItemMap(locationSearchResult.locationsWithDistance.map(_.location))
        items = locationSearchResult.locationsWithDistance map LocationsSearchResponseItem(locationCategoryMap, request, lang)
      } yield LocationsSearchResponse(items, locationSearchResult.locationsCount)
      result pipeTo sender

    case GetFavoriteLocations(request, lang) =>
      implicit val lng = lang
      val result = for {
        locationsId <- getFavoriteLocationsIdFromGraph(request)
        locationsWithCount <- getFavoriteLocations(request, locationsId, lang)
        locationsCategoryItemMap <- getLocationCategoryItemMap(locationsWithCount.locations)
      } yield GetFavoriteLocationsResponse(
          locationsWithCount.locations map FavoriteLocationItem(locationsCategoryItemMap, lang),
          locationsWithCount.locationsCount
        )
      result pipeTo sender

    case GetProductCategoriesOfLocation(locationId, ctx) =>
      implicit val lang = ctx.lang
      locationDataActor.ask(LocationDataActor.GetProductCategories(locationId)).mapTo[Seq[AdditionalLocationCategory]]
        .map(categories =>
          GetProductCategoriesResponse(categories.map( ctg => GetProductCategoriesResponseItem(ctg)))
          ) pipeTo sender

    case GetLocationMoreInfo(locationId, ctx) =>
      implicit val lang = ctx.lang
      locationDataActor.ask(LocationDataActor.GetLocation(locationId)).mapTo[Location]
        .map(l => GetLocationMoreInfoResponse(LocationMoreInfoItem(l))) pipeTo sender

    case FindUserEventLocations(ids, lang, imageDimension) => findLocations(ids) map (_ map UserEventLocation(lang, imageDimension)) pipeTo sender

    case GetLocations(ids: Seq[LocationId], lang) => findLocations(ids.toSet) map LocationItems(lang) pipeTo sender

    case GetProductMoreInfoResponseLocation(id, lang) =>
      getLocation(id) map ProductMoreInfoResponseLocation(lang) pipeTo sender

    case GetProductLocationItem(id, lang) => getLocation(id) map ProductLocationItem(lang) pipeTo sender

    case GetPromotionLocationFullItem(id, lang) => getLocation(id) map PromotionFullLocation(lang) pipeTo sender

    case GetLocationItem(id, lang) =>
      (locationDataActor ? LocationDataActor.GetLocation(id)).mapTo[Location].map { location =>
        UserEventLocation(lang, Images.Location.Full.Self.Media)(location)
      } pipeTo sender

    case GetLocationStatistics(locationId) =>
      (locationDataActor ? LocationDataActor.GetStatistics(locationId)).mapTo[LocationCounters].map { locationCounters =>
        LocationStatisticsResponse(LocationFullStatistics(locationCounters))
      } pipeTo sender
  }

  def getFavoriteLocations(request: GetFavoriteLocationsRequest, locationIds: Seq[LocationId], lang: Lang): Future[LocationsWithCountResult] = {
    locationDataActor.ask(LocationDataActor.GetFavoriteLocationsBy(request, locationIds, lang)).mapTo[LocationsWithCountResult]
  }

  def getLocationsSearchByCoordinates(request: LocationsSearchRequest, lang: Lang): Future[LocationSearchResult] = {
    locationDataActor.ask(LocationDataActor.GetLocationsSearchByCoordinates(request, lang)).mapTo[LocationSearchResult]
  }

  def getIsFavorite(userId: UserId, location: Location, viewType: ShowType): Future[Boolean] = {
    if(viewType != ShowType.Mini)
      graphGetFavoritesActor.ask(new IsFavoriteLocation(userId, location.locationId)).mapTo[Boolean]
    else
      Future successful false
  }

  def getLocationPhotos(location: Location, viewType: ShowType): Future[Seq[LocationPhoto]] = {
    val photos = if(viewType == ShowType.Mini) location.lastPhotos.lastOption.map(Seq(_)).getOrElse(Seq.empty) else location.lastPhotos
    locationPhotoDataActor.ask(GetLocationPhotos(photos)).mapTo[Seq[LocationPhoto]]
  }

  def getLocationCheckinInfo(locationId: LocationId, userId: UserId, viewType: ShowType): Future[LocationCheckinInfoItem] = {
    if(viewType == ShowType.Short) {
      checkinReadActor.ask(CheckinReadActor.GetLocationCheckinInfoItem(locationId, userId)).mapTo[LocationCheckinInfoItem]
    }
    else Future successful LocationCheckinInfoItem()
  }

  def getSelfSubscribed(userId: UserId, locationId: LocationId, viewType: ShowType): Future[Boolean] =
    if(viewType == ShowType.None)
      (locationSubscriptionDataActor ? LocationSubscriptionDataActor.ExistLocationSubscription(userId, locationId)).mapTo[Boolean]
    else
        Future successful false

  def getLocation(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocation(id)).mapTo[Location]

  def getLocationForAdmin(id: LocationId): Future[Location] =
    (locationDataActor ? LocationDataActor.GetLocationWithAnyLifeCycleStatus(id)).mapTo[Location]

  def getLocationCategories(locations: Seq[Location], lang: Lang): Future[Seq[LocationCategoryItem]] = {
    val categoryIds = locations.flatMap(_.locationCategories).toSet
    (locationCategoryActor ? LocationCategoryActor.GetLocationCategoryItems(categoryIds.toSeq, lang)).mapTo[Seq[LocationCategoryItem]]
  }

  def getLocationCategoryItemMap(locations: Seq[Location])(implicit lang: Lang): Future[Map[LocationCategoryId, LocationCategoryItem]] =
    getLocationCategories(locations, lang) map (_ toMapId (_.id))

  def getFavoriteLocationsIdFromGraph(flr: GetFavoriteLocationsRequest): Future[Seq[LocationId]] = {
    getFavoriteLocationsGraphActor.ask(new GraphGetFavoriteLocations(flr.userId))
      .mapTo[GraphLocations].map(gl => gl.getIds.asScala.toSeq)
  }

  def findLocations(ids: Set[LocationId]): Future[Seq[Location]] =
    (locationDataActor ? LocationDataActor.FindLocations(ids)).mapTo[Seq[Location]]
}

case class GetProductCategoriesResponse(@JsonProperty("productcategories") productCategories: Seq[GetProductCategoriesResponseItem]) extends SuccessfulResponse

case class GetProductCategoriesResponseItem(id: AdditionalLocationCategoryId, name: String)
object GetProductCategoriesResponseItem {
  def apply(category: AdditionalLocationCategory)(implicit lang: Lang): GetProductCategoriesResponseItem =
    GetProductCategoriesResponseItem(category.id, category.name.localized getOrElse "")
}

case class GetFavoriteLocationsResponse(locations: Seq[FavoriteLocationItem], @JsonProperty("locationscount")locationsCount: Option[Int]) extends SuccessfulResponse

case class FavoriteLocationItem
(
  id: LocationId,
  name: String,
  @JsonProperty("openinghours") openingHours: String,
  address: LocationAddressItem,
  media: MediaUrl,
  @JsonProperty("coords") coordinates: Coordinates,
  promotion: Option[Boolean],
  category: LocationCategoryItem,
  statistics: GetLocationsStatisticItem
  )

object FavoriteLocationItem {
  def apply(locationsCategoryItemMap: Map[LocationCategoryId, LocationCategoryItem], lang: Lang)(location: Location): FavoriteLocationItem = FavoriteLocationItem(
    id = location.id,
    name = location.name.localized(lang) getOrElse "",
    openingHours = location.openingHours.localized(lang) getOrElse "",
    address = LocationAddressItem(location.contact.address),
    media = location.getMainLocationMediaUrl(Images.Favoritelocations.Full.Self.Media),
    coordinates = location.contact.address.coordinates,
    promotion = location.hasPromotions,
    category = location.oneOfLocationCategoryId map locationsCategoryItemMap.apply get,
    statistics = GetLocationsStatisticItem(location)
  )
}

case class LocationsSearchResponse(locations: Seq[LocationsSearchResponseItem], @JsonProperty("locationscount")locationsCount: Option[Int]) extends SuccessfulResponse
case class LocationsSearchResponseItem(
  id: LocationId,
  name: String,
  @JsonProperty("openinghours")openingHours: String,
  address: LocationAddressItem,
  media: MediaUrl,
  @JsonProperty("coords") coordinates: Coordinates,
  promotion: Option[Boolean],
  category: LocationCategoryItem,
  distance: Double,
  statistics: Option[LocationSearchResponseStatistic] = None
)

object LocationsSearchResponseItem {

  def apply
  (
    locationCategoryMap: Map[LocationCategoryId, LocationCategoryItem],
    request: LocationsSearchRequest,
    lang: Lang
    )(locationWithDistance: LocationWithDistance) : LocationsSearchResponseItem =
  {
    val l = locationWithDistance.location
    def locationCategoryItem = l.oneOfLocationCategoryId map locationCategoryMap

    LocationsSearchResponseItem(
      id = l.id,
      name = l.name.localized(lang) getOrElse "",
      openingHours = l.openingHours.localized(lang) getOrElse "",
      address = LocationAddressItem(l.contact.address),
      media = l.getMainLocationMediaUrl(Images.Locationssearch.Full.Location.Media),
      coordinates = l.contact.address.coordinates,
      promotion = l.hasPromotions,
      category = locationCategoryItem.get,
      distance = locationWithDistance.distance.get,
      statistics = if(request.includeInResponseStatistics)
        Option(LocationSearchResponseStatistic(l.statistics.favoritePlaceCount, l.statistics.subscribersCount)) else None
    )
  }
}
case class LocationSearchResponseStatistic(@JsonProperty("favoritescount")favoritesCount: Int, @JsonProperty("subscriberscount")subscribersCount: Int)

case class GetLocationInfoRequest(locationId: LocationId, view: Option[ShowType], usersLimit: Int)
{
  def getView = view getOrElse ShowType.None
}

case class GetFavoriteLocationsRequest
(
  userId: UserId,
  region: RegionId,
  @JsonProperty("category") categoryId: LocationCategoryId,
  entitiesParam: Option[Set[LocationSearchViewType]],
  offsetLimit: OffsetLimit
) extends LocationSearchRequestBase with Validatable {
  check

  def validate(ctx: ValidationContext): Unit = {
    if(responseIncludesLocationsQty && offsetLimit.offset != 0) ctx.fail( "Paging is denied with such entities parameter")
  }
}

trait LocationSearchRequestBase {
  def entitiesParam: Option[Set[LocationSearchViewType]]
  def entities = entitiesParam.getOrElse(Set(LocationSearchViewType.Locations, LocationSearchViewType.LocationsCount))
  def responseIncludesLocationsQty = entities contains LocationSearchViewType.LocationsCount
  def responseIncludesLocations = entities contains LocationSearchViewType.Locations

}

case class LocationsSearchRequest
(
  category: LocationCategoryId,
  radius: Option[Int],
  @JsonProperty("lon") longitude: Double,
  @JsonProperty("lat") latitude: Double,
  sort: Option[LocationSearchSortType],
  entitiesParam: Option[Set[LocationSearchViewType]],
  offsetLimit: OffsetLimit
  ) extends LocationSearchRequestBase with Validatable
{
  check

  def validate(ctx: ValidationContext) {
    if(radius.isDefined && (radius.get < 1000 || radius.get > 5000) ) ctx.fail("Radius must be in the range from 1000 to 5000 meters")
    if(responseIncludesLocationsQty && offsetLimit.offset != 0) ctx.fail( "Paging is denied with such entities parameter")
  }
  val settings = Settings(SharedActorSystem.sharedMainActorSystem)
  import settings._

  def sortType = sort.getOrElse(LocationSearchSortType.Name)
  def point = new Point(longitude, latitude)
  def latLng = new LatLng(latitude, longitude) //TODO reuse Coordinates.distanceIn
  def includeInResponseStatistics = entities contains LocationSearchViewType.Locations
  def finalRadius = radius.getOrElse(GeoSearch.LocationSearch.RadiusMax)
}

case class GetLocationMoreInfoResponse(location: LocationMoreInfoItem) extends SuccessfulResponse

case class LocationMoreInfoItem(id: LocationId, details: LocationMoreInfoDetailsItem)

object LocationMoreInfoItem {
  def apply(location: Location)(implicit lang: Lang): LocationMoreInfoItem =  LocationMoreInfoItem(location.locationId, LocationMoreInfoDetailsItem(location.description.localized getOrElse ""))
}

case class LocationMoreInfoDetailsItem(description: String)

case class UserEventLocation
(
  @JsonProperty("id") id: LocationId,
  @JsonProperty("name") name: String,
  @JsonProperty("media") media: MediaUrl,
  @JsonProperty("address") address: LocationAddressItem,
  @JsonProperty("coords") coordinates: Coordinates
  )

object UserEventLocation {
  def apply(lang: Lang, imageDimension: String)(l: Location): UserEventLocation = UserEventLocation(
    id = l.locationId,
    name = l.name.localized(lang) getOrElse "",
    media = l.getMainLocationMediaUrl(imageDimension),
    address = LocationAddressItem(l.contact.address),
    coordinates = l.contact.address.coordinates
  )

}

case class LocationItems(locations: Seq[LocationItem]) extends SuccessfulResponse

object LocationItems {
  def apply(lang: Lang)(locations: Seq[Location]): LocationItems = LocationItems(locations map LocationItem(lang))
}

case class LocationItem
(
  id: LocationId,
  name: String,
  @JsonProperty("favoritescount") favoritesCount: Int,
  @JsonProperty("presentscount") presentsCount: Int,
  address: LocationAddressItem,
  media: MediaUrl,
  @JsonProperty("openinghours") openingHours: String
  )

object LocationItem {

  def apply(lang: Lang)(location: Location): LocationItem = LocationItem(
    id = location.id,
    name = location.name.localized(lang) getOrElse "",
    favoritesCount = location.statistics.favoritePlaceCount,
    presentsCount = location.statistics.presentsCount,
    address = LocationAddressItem(location.contact.address),
    media = location.getMainLocationMediaUrl(""), // not attached to request
    openingHours = location.openingHours(lang)
  )
}

case class LocationInfo(location: LocationInfoItem) extends SuccessfulResponse
case class LocationInfoItem(
                             id: LocationId,
                             name: String,
                             openinghours: String,
                             coords: Coordinates,
                             address: LocationAddressItem,
                             @JsonProperty("isfavorite")isFavorite: Option[Boolean],
                             checkins: LocationCheckinInfoItem,
                             media: Seq[MediaShortItem] = Nil,
                             @JsonProperty("lastphotos")lastPhotos: Seq[MediaItem],
                             statistics: PresentsCount
                             ) extends UnmarshallerEntity

object LocationInfoItem {
  def apply(location: Location,
            isLocationFavorite: Boolean,
            locationPhotos: Seq[LocationPhoto],
            locationCheckinInfo: LocationCheckinInfoItem)(implicit lang: Lang): LocationInfoItem = {
    val photosMap = locationPhotos.toMapId(_.id)
    LocationInfoItem(
    location.id,
    location.name.localized getOrElse "",
    location.openingHours.localized getOrElse "",
    location.contact.address.coordinates,
    address = LocationAddressItem(location.contact.address),
    isFavorite = Option(isLocationFavorite).filter(identity),
    checkins = locationCheckinInfo,
    media = location.locationMedia.map(lm => MediaShortItem(lm, Images.Location.Short.Self.Media)),
    lastPhotos = location.lastPhotos.map { p =>
      val photo = photosMap(p)
      MediaItem(photo.id.id, photo.fileUrl.asUrl(Images.Location.Short.Self.Lastphotos))
    },
    statistics = PresentsCount(location.statistics.presentsCount)
  )
  }
}

case class LocationFullInfo(location: LocationInfoFullItem) extends SuccessfulResponse

case class LocationInfoFullItem(
                                 id: LocationId,
                                 name: String,
                                 openinghours: String,
                                 details: LocationDetails,
                                 coords: Coordinates,
                                 address: LocationAddressItem,
                                 @JsonProperty("isfavorite")isFavorite: Option[Boolean],
                                 media: Seq[MediaShortItem] = Nil,
                                 @JsonProperty("mainphone") phone: Option[LocationPhone],
                                 categories: Seq[LocationCategoryId],
                                 url: Option[String],
                                 @JsonProperty("lastphotos")lastPhotos: Seq[MediaItem],
                                 statistics: LocationFullStatistics,
                                 @JsonProperty("selfsubscribed") selfSubscribed: Option[Boolean]
                                 )


object LocationInfoFullItem {

  def apply(location: Location,
            isLocationFavorite: Boolean,
            locationPhotos: Seq[LocationPhoto],
            selfSubscribed: Boolean)(implicit lang: Lang): LocationInfoFullItem =
    LocationInfoFullItem(
      id = location.id,
      name = location.name.localized getOrElse "",
      openinghours = location.openingHours.localized getOrElse "",
      details = LocationDetails(location.description.localized getOrElse ""),
      coords = location.contact.address.coordinates,
      address = LocationAddressItem(location.contact.address),
      isFavorite = Option(isLocationFavorite).filter(identity),
      media = location.locationMedia.map(lm => MediaShortItem(lm, Images.Location.Full.Self.Media)),
      lastPhotos = locationPhotos.reverse.map(lp => MediaItem(lp.id.id, lp.fileUrl.asUrl(Images.Location.Full.Self.Lastphotos))),
      phone = location.contact.phones.find(_.purpose == Some("main")).map(LocationPhone(_)),
      categories = location.locationCategories,
      url = location.contact.url,
      statistics = LocationFullStatistics(location.statistics),
      selfSubscribed = Option(selfSubscribed).filter(identity)
    )

}

case class LocationMiniInfo(location: LocationInfoMiniItem) extends SuccessfulResponse

case class LocationInfoMiniItem(
                                 id: LocationId,
                                 name: String,
                                 openinghours: String,
                                 address: LocationAddressItem,
                                 @JsonProperty("mainmedia") media: MediaUrl,
                                 @JsonProperty("lastphoto") lastPhoto: MediaItem
                                 )

object LocationInfoMiniItem {

  def apply(location: Location, locationPhotos: Seq[LocationPhoto])(implicit lang: Lang): LocationInfoMiniItem =
    LocationInfoMiniItem(
      id = location.id,
      name = location.name.localized getOrElse "",
      openinghours = location.openingHours.localized getOrElse "",
      address = LocationAddressItem(location.contact.address),
      media = location.getMainLocationMediaUrl(Images.Location.Mini.Self.Main),
      lastPhoto = locationPhotos.headOption.map(lp => MediaItemDto(lp.id.id, lp.fileUrl)).asMediaItem(Images.Location.Mini.Self.Lastphoto, LocationDefaultUrlType)
    )

}


case class PresentsCount(@JsonProperty("presentscount")presentsCount: Long)

case class LocationFullStatistics(
                                  @JsonProperty("presentscount") presentsCount: Long,
                                  @JsonProperty("photoalbumscount") photoalbumsCount: Long,
                                  @JsonProperty("reviewscount") reviewsCount: Long,
                                  @JsonProperty("subscriberscount") subscribersCount: Long,
                                  @JsonProperty("favoritescount") favoritesCount: Long
                                   )

object LocationFullStatistics {

  def apply(statistics: LocationCounters): LocationFullStatistics =
    LocationFullStatistics(statistics.presentsCount, statistics.photoalbumsCount, statistics.reviewsCount, statistics.subscribersCount, statistics.favoritePlaceCount)

}

case class LocationAdminInfo(location: LocationInfoAdminItem) extends SuccessfulResponse

case class LocationInfoAdminItemStatistics(presentscount: Int, photoalbumscount: Int,reviewscount: Int, subscriberscount: Int) extends UnmarshallerEntity

case class LocationInfoAdminItem(
                                 id: LocationId,
                                 name: String,
                                 openinghours: String,
                                 details: LocationDetails,
                                 coords: Coordinates,
                                 address: LocationFullAddressItem,
                                 media: Seq[MediaShortItem] = Nil,
                                 @JsonProperty("mainphone") phone: Option[LocationPhone],
                                 categories: Seq[LocationCategoryId],
                                 statistics: LocationInfoAdminItemStatistics,
                                 url: Option[String]
                                 //moderation: ModerationStatusItem TODO will be added later
                                 )

object LocationInfoAdminItem {

  def apply(location: Location, region: Region)(implicit lang: Lang): LocationInfoAdminItem = {
    LocationInfoAdminItem(
      id = location.id,
      name = location.name.localized getOrElse "",
      openinghours = location.openingHours.localized getOrElse "",
      coords = location.contact.address.coordinates,
      details = LocationDetails(location.description.localized getOrElse ""),
      address = LocationFullAddressItem(location.contact.address, region),
      media = location.locationMedia.map(lm => MediaShortItem(lm, Images.Location.Adm.Self.Media)),
      phone = location.contact.phones.find(_.purpose == Some("main")).map(LocationPhone(_)),
      categories = location.locationCategories,
      statistics = LocationInfoAdminItemStatistics(
        presentscount = location.statistics.presentsCount,
        photoalbumscount = location.statistics.photoalbumsCount,
        reviewscount = location.statistics.reviewsCount,
        subscriberscount = location.statistics.subscribersCount
      ),
      url = location.contact.url
    )
  }

}

case class CheckinsStatistic(
                              @JsonProperty("all_count") allCount: Int,
                              @JsonProperty("boys_count") boysCount: Int,
                              @JsonProperty("girls_count") girlsCount: Int,
                              @JsonProperty("friends_here") friendsCount: Int
                              )

case class ProductLocationItem
(
  id: LocationId,
  name: String,
  media: MediaUrl,
  address: LocationAddressWithPhoneItem,
  openinghours: String,
  coords: Coordinates
  )

object ProductLocationItem {
  def apply(lang: Lang)(location: Location): ProductLocationItem = ProductLocationItem(
    id = location.id,
    name = location.name.localized(lang) getOrElse "",
    media = location.getMainLocationMediaUrl(Images.Product.Full.Location.Media),
    address = LocationAddressWithPhoneItem(location.contact),
    openinghours = location.openingHours.localized(lang) getOrElse "",
    coords = location.contact.address.coordinates
  )
}

case class ProductMoreInfoResponseLocation
(
  id: LocationId,
  name: String,
  @JsonProperty("openinghours") openingHours: String,
  description: String,
  address: LocationAddressWithPhoneItem,
  web: String
  )

object ProductMoreInfoResponseLocation {
  def apply(lang: Lang)(location: Location): ProductMoreInfoResponseLocation = ProductMoreInfoResponseLocation(
    id = location.locationId,
    name = location.name.localized(lang) getOrElse "",
    openingHours = location.openingHours.localized(lang) getOrElse "",
    description = location.description.localized(lang) getOrElse "",
    address = LocationAddressWithPhoneItem(location.contact),
    web = location.contact.url.getOrElse("")
  )
}

case class LocationAddressWithPhoneItem
(
  country: String,
  region: String,
  street: String,
  phone: String
  )

object LocationAddressWithPhoneItem {
  def apply(lc: LocationContact): LocationAddressWithPhoneItem = LocationAddressWithPhoneItem(
    country = lc.address.country,
    region = lc.address.regionName,
    street = lc.address.street,
    phone = lc.mainPhone.map(_.fullNumber) getOrElse ""
  )
}

trait PromotionLocation

case class PromotionShortLocation
(
  id: LocationId,
  name: String
  ) extends PromotionLocation

object PromotionShortLocation {
  def apply(location: com.tooe.core.db.mongo.domain.promotion.Location)(lang: Lang): PromotionShortLocation =
    PromotionShortLocation(location.location, location.name.localized(lang).getOrElse(""))
}

case class PromotionFullLocation
(
  id: LocationId,
  name: String,
  address: LocationAddressItem,
  phone: String,
  @JsonProperty("coords") coordinates: Coordinates,
  media: MediaUrl
  ) extends PromotionLocation

object PromotionFullLocation {
  def apply(lang: Lang)(l: Location): PromotionFullLocation = PromotionFullLocation(
    l.id,
    l.name.localized(lang).getOrElse(""),
    LocationAddressItem(l.contact.address),
    l.contact.phones.headOption.map(p => p.countryCode + " " + p.number).getOrElse("не указан"),
    l.contact.address.coordinates,
    l.getMainLocationMediaUrl("")
  )
}

case class LocationStatisticsResponse(statistics: LocationFullStatistics) extends SuccessfulResponse

case class LocationSearchResult(locationsWithDistance: Seq[LocationWithDistance], @JsonProperty("locationscount") locationsCount: Option[Int])

case class GetLocationsForInvitationRequest
(
  region: RegionId,
  category: Option[LocationCategoryId],
  name: Option[String],
  @JsonProperty("userids") userIds: Option[Set[UserId]],
  sort: Option[NamePopularitySortType],
  entitiesParam: Option[Set[LocationSearchViewType]],
  offsetLimit: OffsetLimit
) extends LocationSearchRequestBase with Validatable
{
  check

  def validate(ctx: ValidationContext) {
    if(name.exists(_.length < 3))
      ctx.fail("Illegal parameters")
    if(userIds.size > 2)
      ctx.fail("Illegal parameters")
    if(!(category.isDefined || name.isDefined))
      ctx.fail("At least one of category and name parameters must be specified")
    if(responseIncludesLocationsQty && offsetLimit.offset != 0) ctx.fail( "Paging is denied with such entities patarameter")
  }
  def sortType = sort.getOrElse(NamePopularitySortType.Name)
}

case class GetLocationsForInvitationResponse
(
  locations: Seq[GetLocationsForInvitationLocationItem], 
  @JsonProperty("locationscount") locationsCount: Option[Int]
) extends SuccessfulResponse

case class GetLocationsForInvitationLocationItem
(
  id: LocationId,
  name: String,
  @JsonProperty("openinghours")openingHours: String,
  address: LocationAddressItem,
  media: MediaUrl,
  coords: Coordinates,
  statistics: LocationForInvitationStatistics,
  categories: Seq[LocationForInvitationCategories]
  )

case class LocationForInvitationStatistics(@JsonProperty("presentscount") presentsCount: Option[Int])
case class LocationForInvitationCategories(id: String, name: String)

object GetLocationsForInvitationLocationItem {
  def apply(l: Location)(implicit lang: Lang, ac: ActorContext) : GetLocationsForInvitationLocationItem =
    GetLocationsForInvitationLocationItem(
      id = l.id,
      name = l.name.localized(lang) getOrElse "",
      openingHours = l.openingHours.localized(lang) getOrElse "",
      address = LocationAddressItem(l.contact.address),
      media = l.getMainLocationMediaUrl(Images.Lregionsearch.Full.Locations.Media),
      coords = l.contact.address.coordinates,
      statistics = LocationForInvitationStatistics(presentsCount = l.statistics.presentsCount.optionNonZero),
      categories = l.locationCategories.map(c => LocationForInvitationCategories(c.id, LocationCategory.categoriesMap(ac)(c).name.localized(lang).getOrElse(""))) ++
        l.additionalLocationCategories.map(ac => LocationForInvitationCategories(ac.id.id.toString, ac.name.localized(lang).getOrElse("")))
    )
}
case class LocationsWithCountResult(locations: Seq[Location], locationsCount: Option[Int])

case class GetLocationsByChainRequest
(
  locationChain: LocationsChainId,
  region: RegionId,
  @JsonProperty("haspromo") hasPromo: Option[Boolean],
  entitiesParam: Option[Set[LocationSearchViewType]],
  offsetLimit: OffsetLimit
) extends LocationSearchRequestBase with Validatable
{
  check

  def validate(ctx: ValidationContext): Unit = {
    if(responseIncludesLocationsQty && offsetLimit.offset != 0) ctx.fail( "Paging is denied with such entities patarameter")
  }
}

case class GetLocationsByChainResponse
(
  locations: Seq[GetLocationsByChainResponseLocationItem],
  @JsonProperty("locationscount") locationsCount: Option[Int]
  ) extends SuccessfulResponse

case class GetLocationsByChainResponseLocationItem
(
  id: LocationId,
  name: String,
  @JsonProperty("openinghours")openingHours: String,
  address: LocationAddressItem,
  media: MediaUrl,
  @JsonProperty("coords") coordinates: Coordinates,
  promotion: Option[Boolean],
  category: LocationCategoryItem,
  statistics: GetLocationsStatisticItem
)

object GetLocationsByChainResponseLocationItem {

  def apply(l: Location, locationCategoryMap: Map[LocationCategoryId, LocationCategoryItem])(implicit lang: Lang) : GetLocationsByChainResponseLocationItem = {
    def locationCategoryItem = l.oneOfLocationCategoryId map locationCategoryMap

    GetLocationsByChainResponseLocationItem(
      id = l.id,
      name = l.name.localized(lang) getOrElse "",
      openingHours = l.openingHours.localized(lang) getOrElse "",
      address = LocationAddressItem(l.contact.address),
      media = l.getMainLocationMediaUrl(Images.Lchainsearch.Full.Location.Media),
      coordinates = l.contact.address.coordinates,
      promotion = l.hasPromotions,
      category = locationCategoryItem.get,
      statistics = GetLocationsStatisticItem(l)
    )
  }
}

case class GetLocationsStatisticItem
(
  @JsonProperty("favoritescount") favoritesCount: Int,
  @JsonProperty("subscriberscount") subscribersCount: Int
)

object GetLocationsStatisticItem {

  def apply(l: Location) : GetLocationsStatisticItem =
    GetLocationsStatisticItem(
      favoritesCount = l.statistics.favoritePlaceCount, 
      subscribersCount = l.statistics.subscribersCount
    )
}


case class GetLocationsForCheckinRequest
(
  @JsonProperty("lon") longitude: Double,
  @JsonProperty("lat") latitude: Double,
  entitiesParam: Option[Set[LocationSearchViewType]],
  offsetLimit: OffsetLimit
  ) extends LocationSearchRequestBase with Validatable
{
  check

  def validate(ctx: ValidationContext): Unit = {
    if(responseIncludesLocationsQty && offsetLimit.offset != 0) ctx.fail( "Paging is denied with such entities patarameter")
  }

  def point = new Point(longitude, latitude)
  def latLng = new LatLng(latitude, longitude)

}

case class GetLocationsForCheckinResponse
(
  locations: Seq[GetLocationsForCheckinResponseLocationItem],
  @JsonProperty("locationscount") locationsCount: Option[Int]
  ) extends SuccessfulResponse

case class GetLocationsForCheckinResponseLocationItem
(
  id: LocationId,
  name: String,
  @JsonProperty("openinghours")openingHours: String,
  address: LocationAddressItem,
  media: MediaUrl,
  @JsonProperty("coords") coordinates: Coordinates,
  promotion: Option[Boolean],
  category: LocationCategoryItem,
  statistics: GetLocationsStatisticItem,
  distance: Option[Double],
  checkin: Option[Object] = None
  )

object GetLocationsForCheckinResponseLocationItem {

  def apply(l: Location, locationCategoryMap: Map[LocationCategoryId, LocationCategoryItem], coords: LatLng)(implicit lang: Lang) : GetLocationsForCheckinResponseLocationItem = {
    def locationCategoryItem = l.oneOfLocationCategoryId map locationCategoryMap

    GetLocationsForCheckinResponseLocationItem(
      id = l.id,
      name = l.name.localized(lang) getOrElse "",
      openingHours = l.openingHours.localized(lang) getOrElse "",
      address = LocationAddressItem(l.contact.address),
      media = l.getMainLocationMediaUrl(Images.Lforcheckinsearch.Full.Location.Media),
      coordinates = l.contact.address.coordinates,
      promotion = l.hasPromotions,
      category = locationCategoryItem.get,
      statistics = GetLocationsStatisticItem(l),
      distance = Option( LatLngTool.distance(coords, l.contact.address.coordinates.toLatLng, LengthUnit.KILOMETER))
    )
  }
}


