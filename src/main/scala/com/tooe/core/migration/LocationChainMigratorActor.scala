package com.tooe.core.migration

import java.util.Date
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.migration.db.domain.MappingCollection._
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.domain._
import com.tooe.core.usecase.locationschain.LocationsChainDataActor
import com.tooe.core.util.Lang
import com.tooe.core.domain.MediaObject
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.domain.CompanyId
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.migration.db.domain.IdMapping
import com.tooe.core.domain.LocationsChainId
import scala.Some
import com.tooe.core.db.mongo.domain.{LocationsChainMedia, LocationsChain}

object LocationChainMigratorActor {
  val Id = 'locationChainMigrator

  case class LegacyLocationChain(legacyid: Int, name: String, description: String, companyid: Int,
                                 registationdate: Date, media: Option[String]) extends UnmarshallerEntity
}

class LocationChainMigratorActor extends MigrationActor {
  import LocationChainMigratorActor._
  def receive = {
    case llc: LegacyLocationChain =>
      saveLocationChain(llc).map(lc =>
        MigrationResponse(DefaultMigrationResult(llc.legacyid, lc.id.id, "locationschain_migrator"))) pipeTo sender
  }

  def saveLocationChain(llc: LegacyLocationChain): Future[LocationsChain] = {
    def upgradeChain(company: CompanyId): Future[LocationsChain] = {
      Future.successful(LocationsChain(LocationsChainId(new ObjectId()),
        name = Map(Lang.ru -> llc.name),
        description = Some(Map(Lang.ru -> llc.description)),
        companyId = company,
        registrationDate = llc.registationdate,
        locationCount = 0,
        locationChainMedia = llc.media.map(m => LocationsChainMedia(MediaObject(MediaObjectId(m), UrlType.MigrationType))).toSeq)) //Once you get here type is http
    }
    for {
      company <- lookupByLegacyId(llc.companyid, company).map(CompanyId)
      chain <- upgradeChain(company)
      result <- (locationChainDataActor ? LocationsChainDataActor.SaveLocationsChain(chain)).mapTo[LocationsChain]
      _ <- idMappingDataActor ? IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId, locationsChain, llc.legacyid, result.id.id))
    } yield {
      llc.media.foreach {
        url =>
          saveUrl(EntityType.locationsChain, result.id.id, url, UrlField.LocationsChainMain)
      }
      result
    }
  }

  lazy val locationChainDataActor = lookup(LocationsChainDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val dictionaryIdMappingActor = lookup(DictionaryIdMappingActor.Id)
}
