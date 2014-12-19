package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.usecase.AppActor
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import java.util.Date
import com.tooe.core.usecase.location_news.{LocationNewsLikeDataActor, LocationNewsDataActor}
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.domain.{LocationsChainId, LocationNewsId, LocationId, UserId}
import com.tooe.core.db.mongo.domain.{Location, LocationNewsLike, LocationNews}
import com.tooe.core.util.Lang
import scala.concurrent.Future
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}
import com.tooe.core.usecase.location.LocationDataActor
import org.bson.types.ObjectId

object LocationNewsMigratorActor {
  final val Id = 'LocationNewsMigrator

  case class LegacyLocationNews
  (
    legacyid: Int,
    locationid: Int,
    content: String,
    comments_enabled: Option[Boolean],
    time: Date,
    likes: Seq[LegacyLocationNewsLike]
    ) extends UnmarshallerEntity

  case class LegacyLocationNewsLike(userid: Int, time: Date)

}

class LocationNewsMigratorActor extends MigrationActor {
  import LocationNewsMigratorActor._

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val locationNewsDataActor = lookup(LocationNewsDataActor.Id)
  lazy val locationNewsLikeDataActor = lookup(LocationNewsLikeDataActor.Id)

  def receive = {
    case legacyLocationNews: LegacyLocationNews =>
      val result = for {
        (news, likes) <- legacyLocationNewsToLocationNewsWithLikes(legacyLocationNews)
        _ <- saveLocationNews(news)
        _ = likes.map(saveLocationLike)
      } yield MigrationResponse(DefaultMigrationResult(legacyLocationNews.legacyid, news.id.id, "locationnews_migrator"))
      result pipeTo sender
  }

  def legacyLocationNewsToLocationNewsWithLikes(news: LegacyLocationNews) =
    for {
      locationId <- lookupByLegacyId(news.locationid, MappingCollection.location).map(LocationId)
      locationNewsId = LocationNewsId()
      newsLikes <- getIdMappings(news.likes.map(_.userid), MappingCollection.user).map(_ zip news.likes)
        .mapInner {
        case (uid, ll) => LocationNewsLike(locationNewsId = locationNewsId, time = ll.time, userId = UserId(uid))
      }
      chain <- getLocationsChainId(locationId)
    } yield {
      val LocNews = LocationNews(
        id = locationNewsId,
        locationId = locationId,
        content = Map(Lang.ru -> news.content),
        commentsEnabled = news.comments_enabled,
        likesCount = news.likes.size,
        lastLikes = newsLikes.takeRight(10).map(_.userId),
        locationsChainId = chain
      )
      (LocNews, newsLikes)
    }

  def saveLocationNews(news: LocationNews) =
    locationNewsDataActor.ask(LocationNewsDataActor.CreateLocationNews(news))

  def getLocationsChainId(locationId: LocationId): Future[Option[LocationsChainId]] = {
    (locationDataActor ? LocationDataActor.GetLocation(locationId)).mapTo[Location]
      .map(loc => loc.locationsChainId)
  }

  def saveLocationLike(like: LocationNewsLike) =
    locationNewsLikeDataActor.ask(LocationNewsLikeDataActor.SaveLike(like))

}
