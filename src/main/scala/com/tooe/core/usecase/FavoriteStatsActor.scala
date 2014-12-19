package com.tooe.core.usecase

import com.tooe.core.domain._
import com.tooe.core.usecase.favorite_stats.FavoriteStatsDataActor
import com.tooe.api.service.SuccessfulResponse
import scala.concurrent.ExecutionContext.Implicits.global
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.db.mongo.domain.{FavoritesInRegion, Region, FavoriteStats, Location}
import com.tooe.core.domain.UserId
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.application.Actors
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.util.Lang
import com.fasterxml.jackson.annotation.JsonProperty

object FavoriteStatsActor {
  val Id = Actors.FavoriteStatsActor

  case class UserFavoredAnother(uid: UserId, loc: LocationId)
  case class GetStatsByUserId(user: UserId)
  case class GetFavoriteRegions(user: UserId, country: CountryId, lang: Lang)
  case class UserRemovedFromFavorite(uid: UserId, loc: LocationId)
}

class FavoriteStatsActor extends AppActor {

  import FavoriteStatsActor._

  def receive = {
    case GetStatsByUserId(user) =>
      (favoriteStatsDataActor ask FavoriteStatsDataActor.GetStatsByUserId(user)).mapTo[Seq[FavoriteStats]].map {
        stats =>
          GetStatsByUserIdResponse(FavoriteStatsResponseStats(stats.map {
            stats =>
              FavoriteStatsResponseCountries(
                countryId = stats.countryId.id,
                favoritesCount = stats.favoritesCount,
                coords = stats.countryCoordinates)
          }))
      } pipeTo sender
    case GetFavoriteRegions(uid, country, lang) =>
      val result = for {
        stats <- getFavoriteStats(uid, country)
        regions <- stats.map(_.regions.map(_.region)).map(Future.traverse(_)(getRegion)).getOrElse(Future.successful(Seq.empty))
        responseItems = regions.map(r => FavoriteRegionStatistics(r.id.id, r.name.localized(lang).getOrElse("")))
      } yield FavoriteRegionsResponse(responseItems)
      result.pipeTo(sender)

    case UserFavoredAnother(uid, loc) =>
      (locationDataActor ? LocationDataActor.GetLocation(loc)).flatMap {
        case loc: Location =>
          val stats = FavoriteStats(userId = uid,
            countryId = loc.contact.address.countryId,
            favoritesCount = 1,
            regions = Seq(FavoritesInRegion(loc.contact.address.regionId, 1)),
            countryCoordinates = loc.contact.address.coordinates)
          favoriteStatsDataActor ? FavoriteStatsDataActor.IncreaseCounterOrCreate(stats)
      }
    case UserRemovedFromFavorite(uid, loc) =>
      (locationDataActor ? LocationDataActor.GetLocation(loc)).mapTo[Location].foreach {
        loc =>
          val stats = FavoriteStats(userId = uid,
            countryId = loc.contact.address.countryId,
            favoritesCount = -1,
            regions = Seq(FavoritesInRegion(loc.contact.address.regionId, -1)),
            countryCoordinates = loc.contact.address.coordinates)
          favoriteStatsDataActor ! FavoriteStatsDataActor.IncreaseCounterOrCreate(stats)
      }
  }

  def getFavoriteStats(uid: UserId, country: CountryId) =
    favoriteStatsDataActor.ask(FavoriteStatsDataActor.FindStatsByUserIdAndCountry(uid, country)).mapTo[Option[FavoriteStats]]

  def getRegion(rid: RegionId) =
    regionDataActor.ask(RegionDataActor.GetRegion(rid)).mapTo[Region]

  lazy val regionDataActor = lookup(RegionDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val favoriteStatsDataActor = lookup(FavoriteStatsDataActor.Id)
}

case class FavoriteRegionsResponse(regions: Seq[FavoriteRegionStatistics]) extends SuccessfulResponse
case class FavoriteRegionStatistics(id: ObjectId, name: String)

case class GetStatsByUserIdResponse(statistics: FavoriteStatsResponseStats) extends SuccessfulResponse
case class FavoriteStatsResponseStats(countries: Seq[FavoriteStatsResponseCountries])
case class FavoriteStatsResponseCountries(@JsonProperty("countryid") countryId: String,
                                          @JsonProperty("favoritescount") favoritesCount: Int,
                                          coords: Coordinates)