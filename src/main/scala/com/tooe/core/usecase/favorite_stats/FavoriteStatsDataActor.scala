package com.tooe.core.usecase.favorite_stats

import com.tooe.core.domain.{CountryId, UserId}
import com.tooe.core.usecase.AppActor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.FavoriteStatsDataService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.FavoriteStats
import com.tooe.core.application.Actors

object FavoriteStatsDataActor {
  val Id = Actors.FavoriteStatsDataActor

  case class GetStatsByUserId(user: UserId)
  case class FindStatsByUserIdAndCountry(user: UserId, cnt: CountryId)
  case class SaveFavoriteStats(fs: FavoriteStats)
  case class IncreaseCounterOrCreate(fs: FavoriteStats)
}

class FavoriteStatsDataActor extends AppActor {

  import FavoriteStatsDataActor._
  lazy val service = BeanLookup[FavoriteStatsDataService]

  def receive = {
    case GetStatsByUserId(user) =>
      Future {
        service.findByUserId(user)
      } pipeTo sender
    case SaveFavoriteStats(fs) =>
      Future {
        service.save(fs)
      }
    case IncreaseCounterOrCreate(fs) =>
      Future {
        service.mergeStatsAndSave(fs)
      } pipeTo sender
    case FindStatsByUserIdAndCountry(user, cnt) =>
      Future {
        service.findByUserAndCountry(user, cnt)
      } pipeTo sender
  }

}