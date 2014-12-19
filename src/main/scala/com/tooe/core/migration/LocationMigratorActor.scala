package com.tooe.core.migration

import com.tooe.core.usecase.UpdateStatisticActor
import com.tooe.core.usecase.location.{PreModerationLocationDataActor, LocationDataActor}
import com.tooe.core.db.graph.{GraphPutLocation, LocationGraphActor}
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.db.graph.msg.GraphPutLocationAcknowledgement
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.migration.db.domain.MappingCollection._
import com.tooe.core.util.Lang
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.usecase.country.CountryDataActor
import java.util.Date
import com.tooe.core.usecase.location_statistics.LocationStatisticsDataActor
import com.tooe.core.domain._
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.Coordinates
import com.tooe.core.migration.IdMappingDataActor.SaveIdMapping
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.RegionId
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.domain.AdditionalLocationCategoryId
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.domain.CountryId
import com.tooe.core.migration.db.domain.{MappingCollection, IdMapping}
import com.tooe.core.usecase.locationschain.{LocationsChainStatsDataActor, LocationsChainDataActor}
import com.tooe.core.db.mongo.domain
import com.tooe.core.usecase.statistics.{ArrayOperation, LocationCategoriesUpdate, UpdateRegionOrCountryStatistic}

object LocationMigratorActor {
  final val Id = 'locationMigrator

  case class LegacyAdditionalLocationCategory(legacyid: Int, name: String)
  case class LegacyCoordinates(lon: Double, lat: Double) {
    def toCoordinates: Coordinates = Coordinates(lon, lat)
  }
  case class LegacyLocation(legacyid: Int, companyid: Int, locationschainid: Option[Int],
                            name: String, registrationdate: Date, openinghours: String,
                            description: String, regionid: Int, street: String, categories: Seq[Int],
                            addcategories: Seq[LegacyAdditionalLocationCategory], coords: LegacyCoordinates,
                            phone: Option[String], activationphone: Option[String], url: Option[String], media: Option[String]) extends UnmarshallerEntity
}

class LocationMigratorActor extends MigrationActor {

  import LocationMigratorActor._

  def receive = {
    case legacyLocation: LegacyLocation =>
      val result = for {
        location <- legacyLocationToLocation(legacyLocation)
        _ <- saveLocation(location)
        locationMod <- saveLocationModeration(location)
        _ <- putLocationToGraph(location)
      } yield {
        legacyLocation.media.foreach {
          url =>
            saveUrl(EntityType.location, location.id.id, url, UrlField.LocationMain)
            saveUrl(EntityType.locationModeration, locationMod.id.id, url, UrlField.LocationMain)
        }
        updateStatisticActor ! UpdateStatisticActor.ChangeLocationsCounter(location.contact.address.regionId, 1, Some(location.contact.address.countryId))
        locationStatisticsDataActor ! LocationStatisticsDataActor.SaveLocationStatistics(LocationStatistics(
          locationId = location.id, registrationDate = legacyLocation.registrationdate, visitorsCount = 0
        ))
        MigrationResponse(DefaultMigrationResult(legacyLocation.legacyid, location.id.id, "location_migrator"))
      }
      result.pipeTo(sender)
  }

  def legacyLocationToLocation(legacyLocation: LegacyLocation): Future[Location] =
    for {
      companyId <- lookupByLegacyId(legacyLocation.companyid, MappingCollection.company).map(CompanyId)
      regionId <- exchangeLegacyRegionId(legacyLocation.regionid)
      region <- getRegion(regionId)
      country <- getCountry(region.countryId)
      capital <- (regionDataActor ? RegionDataActor.FindCapital(country.id)).mapTo[Option[Region]]
      chain <- mapLocationsChainId(legacyLocation)(country, region, capital)
      locationCategories <- Future.traverse(legacyLocation.categories)(exchangeLegacyLocationCategoryId)
      locationId <- exchangeLegacyLocationId(legacyLocation.legacyid)
      locationAddCategories <- Future.traverse(legacyLocation.addcategories)(i =>
        exchangeLegacyLocationAddCategoryId(i.legacyid, locationId).map(alci => AdditionalLocationCategory(alci, Map(Lang.ru -> i.name))))
    } yield Location(
      id = locationId,
      companyId = companyId,
      locationsChainId = chain,
      name = Map(Lang.ru -> legacyLocation.name),
      description = Map(Lang.ru -> legacyLocation.description),
      openingHours = Map(Lang.ru -> legacyLocation.openinghours),
      locationMedia = legacyLocation.media.map(lm => LocationMedia(url = MediaObject(url = MediaObjectId(lm), Some(UrlType.http)),
        mediaType = "f",
        purpose = Some("main"))).toSeq,
      contact = LocationContact(
        address = LocationAddress(
          coordinates = Coordinates(legacyLocation.coords.lon, legacyLocation.coords.lat),
          regionId = regionId,
          regionName = region.name(Lang.ru),
          countryId = country.id,
          country = country.name(Lang.ru),
          street = legacyLocation.street
        ),
        phones = legacyLocation.phone.map(phone => Phone(countryCode = country.phoneCode, number = phone, purpose = Some("main"))).toList :::
          legacyLocation.activationphone.map(ap => Phone(countryCode = country.phoneCode, number = ap, purpose = Some("activation"))).toList,
        url = legacyLocation.url
      ),
      locationCategories = locationCategories,
      additionalLocationCategories = locationAddCategories,
      lifecycleStatusId = None // TODO #2461 #2471
    )

  def mapLocationsChainId(legacyLocation: LegacyLocation)(cnt: Country, reg: Region, capital: Option[Region]): Future[Option[LocationsChainId]] = legacyLocation.locationschainid.map {
    id =>
      def createStats(cid: LocationsChainId) = LocationsChainStats(
        chainId = cid,
        countryId = cnt.id,
        locationsCount = 1,
        regions = Seq(LocationsInRegion(reg.id, 1)),
        coordinates = capital.flatMap(c => Option(c.coordinates)).getOrElse(legacyLocation.coords.toCoordinates))
      for {
        cid <- lookupByLegacyId(id, MappingCollection.locationsChain).map(LocationsChainId)
        stats = createStats(cid)
        updated <- (locationsChainStatsDataActor ? LocationsChainStatsDataActor.IncreaseStatsCountersOrCreate(stats)).mapTo[LocationsChainStats]
      } yield cid
  }

  def getRegion(regionId: RegionId) =
    regionDataActor.ask(RegionDataActor.GetRegion(regionId)).mapTo[Region]

  def getCountry(countryId: CountryId) =
    countryDataActor.ask(CountryDataActor.GetCountry(countryId)).mapTo[Country]

  def saveLocation(location: Location) = {
    val update = location.locationCategories.map(c =>
      UpdateRegionOrCountryStatistic(
        locationCategoriesUpdate = Some(LocationCategoriesUpdate(c, ArrayOperation.PushToSet))))
    updateStatistics(location.contact.address.regionId, update)
    locationDataActor.ask(LocationDataActor.SaveLocation(location)).mapTo[Location]
  }

  def saveLocationModeration(loc: Location) = {
    val modLoc = domain.PreModerationLocation(
      id = PreModerationLocationId(),
      companyId = loc.companyId,
      locationsChainId = loc.locationsChainId,
      name = loc.name,
      description = loc.description,
      openingHours = loc.openingHours,
      contact = loc.contact,
      locationCategories = loc.locationCategories,
      additionalLocationCategories = loc.additionalLocationCategories,
      locationMedia = loc.locationMedia,
      lifecycleStatusId = loc.lifecycleStatusId,
      publishedLocation = Some(loc.id),
      moderationStatus = PreModerationStatus(
        status = ModerationStatusId.Active,
        message = Some("Migration"),
        adminUser = None,
        time = Some(new Date)))

    preModLocationDataActor.ask(PreModerationLocationDataActor.SaveLocation(modLoc)).mapTo[PreModerationLocation]
  }

  def putLocationToGraph(location: Location) =
    locationGraphActor.ask(new GraphPutLocation(location.locationId)).mapTo[GraphPutLocationAcknowledgement]

  def exchangeLegacyRegionId(lid: Int) =
    dictionaryIdMappingActor.ask(DictionaryIdMappingActor.GetRegionId(lid)).mapTo[RegionId]

  def exchangeLegacyLocationCategoryId(lid: Int) =
    dictionaryIdMappingActor.ask(DictionaryIdMappingActor.GetLocationCategory(lid)).mapTo[LocationCategoryId]

  def exchangeLegacyLocationId(lid: Int) =
    idMappingDataActor.ask(SaveIdMapping(IdMapping(collection = location, legacyId = lid, newId = new ObjectId()))).mapTo[IdMapping].map(idm => LocationId(idm.newId))

  def exchangeLegacyLocationAddCategoryId(lid: Int, locationId: LocationId) =
    idMappingDataActor.ask(SaveIdMapping(IdMapping(collection = locationAddCategories, legacyId = lid, newId = new ObjectId(), ownerNewId = Option(locationId.id))))
      .mapTo[IdMapping].map(idm => AdditionalLocationCategoryId(idm.newId))

  lazy val preModLocationDataActor = lookup(PreModerationLocationDataActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val locationGraphActor = lookup(LocationGraphActor.Id)
  lazy val dictionaryIdMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val locationStatisticsDataActor = lookup(LocationStatisticsDataActor.Id)
  lazy val locationsChainDataActor = lookup(LocationsChainDataActor.Id)
  lazy val locationsChainStatsDataActor = lookup(LocationsChainStatsDataActor.Id)
}