package com.tooe.core

import scala.concurrent.Future
import com.tooe.core.migration.db.domain.MappingCollection._
import org.bson.types.ObjectId
import com.tooe.core.util.Lang
import com.tooe.core.main.SharedActorSystem
import com.tooe.extensions.scala.Settings
import com.tooe.core.usecase.AppActor
import akka.actor.ActorRef
import com.tooe.core.usecase.urls.UrlsDataActor
import com.tooe.core.domain._
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.usecase.country.CountryDataActor
import com.tooe.core.db.mongo.domain.Region
import scala.Some
import com.tooe.core.domain.RegionId
import com.tooe.core.usecase.statistics.UpdateRegionOrCountryStatistic
import com.tooe.core.domain.MediaObjectId
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.db.mongo.domain.Urls

package object migration {
  private[migration] implicit val lang = Lang.ru
  private[migration] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  implicit def optOfFut2futOfOpt[A](value: Option[Future[A]]): Future[Option[A]] = Future.sequence(value.toTraversable).map(_.headOption)

  implicit class futureInnerMapper[A](val value: Future[Seq[A]]) extends AnyVal {
    def mapInner[B](f: A => B): Future[Seq[B]] = value.map(_.map(f))
  }

  trait MigrationActor extends AppActor {
    def lookupByLegacyId(lid: Int, collection: MappingCollection, owner: Option[ObjectId] = None): Future[ObjectId] = {
      (idMappingDataActor ? IdMappingDataActor.GetIdMapping(lid, collection, ownerNewId = owner)).mapTo[IdMapping].map(_.newId)
    }

    def getIdMappings(lids: Seq[Int], collection: MappingCollection): Future[Seq[ObjectId]] = {
      (idMappingDataActor ? IdMappingDataActor.GetSeqMappings(lids, collection)).mapTo[Seq[ObjectId]]
    }

    def saveUrl(entityType: EntityType, entityId: ObjectId, url: String, field: String) = {
      val save = Urls(entityType = entityType,
        entityId = entityId,
        mediaId = MediaObjectId(url),
        entityField = field match {
          case "" => None
          case st: String => Some(st)
        },
        urlType = UrlType.MigrationType)
      urlsDataActor ! UrlsDataActor.SaveUrls(save)
    }

    def updateStatistics(regionId: RegionId, stats: Seq[UpdateRegionOrCountryStatistic]) {
      for {
        region <- (regionDataActor ? RegionDataActor.GetRegion(regionId)).mapTo[Region]
        countryId = region.countryId
      } {
        stats.foreach {
          st =>
            countryDataActor ! CountryDataActor.UpdateStatistics(countryId, st)
            regionDataActor ! RegionDataActor.UpdateStatistics(regionId, st)
        }
      }
    }

    val regionDataActor: ActorRef = lookup(RegionDataActor.Id)
    val countryDataActor: ActorRef = lookup(CountryDataActor.Id)
    val idMappingDataActor: ActorRef = lookup(IdMappingDataActor.Id)
    val urlsDataActor: ActorRef = lookup(UrlsDataActor.Id)
  }

  private[this] implicit val timeout = Settings(SharedActorSystem.sharedMainActorSystem).DEFAULT_ACTOR_TIMEOUT

  object UrlField {
    val CompanyMain = "cm"
    val LocationsChainMain = "lcm"
    val LocationMain = "lm"
    val LocationPhotoAlbumMain = "lpam"
    val LocationPhoto = "lph"
    val UserPhotoAlbumMain = "pam"
    val UserPhoto = "up"
    val ProductMain = "prm"
    val UserMediaBackground = "um.bg"
    val UserMediaForeground = ""
  }
}
