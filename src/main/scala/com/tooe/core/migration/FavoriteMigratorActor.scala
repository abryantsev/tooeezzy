package com.tooe.core.migration

import com.tooe.core.usecase.{FavoriteStatsActor, UpdateStatisticActor}
import scala.concurrent.Future
import com.tooe.core.migration.db.domain.MappingCollection._
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.db.graph.GraphPutFavoritesActor
import com.tooe.core.db.graph.msg.GraphPutFavorite
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.domain.UserId
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.location.LocationDataActor

object FavoriteMigratorActor {
  val Id = 'favoriteMigratorActor

  case class LegacyFavorite(legacyid: Int, locationids: Seq[Int]) extends UnmarshallerEntity
}

class FavoriteMigratorActor extends MigrationActor {
  import FavoriteMigratorActor._

  def receive = {
    case lf: LegacyFavorite =>
      val future = for {
        uid <- lookupByLegacyId(lf.legacyid, user).map(UserId)
        _ <- saveArrows(uid, lf.locationids)
      } yield MigrationResponse(DefaultMigrationResult(lf.legacyid, new ObjectId(), "favorite_migrator"))
      future pipeTo sender
  }

  def saveArrows(uid: UserId, locations: Seq[Int]): Future[Any] =
    getIdMappings(locations, MappingCollection.location).mapInner {
      lid =>
        updateStatisticsActor ! UpdateStatisticActor.ChangeLocationFavoritesCounters(LocationId(lid), uid, 1)
        favoriteStatsActor ! FavoriteStatsActor.UserFavoredAnother(uid, LocationId(lid))
        graphPutFavoritesActor ? new GraphPutFavorite(uid, LocationId(lid))
    }

  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val favoriteStatsActor = lookup(FavoriteStatsActor.Id)
  lazy val updateStatisticsActor = lookup(UpdateStatisticActor.Id)
  lazy val graphPutFavoritesActor = lookup(GraphPutFavoritesActor.Id)
}