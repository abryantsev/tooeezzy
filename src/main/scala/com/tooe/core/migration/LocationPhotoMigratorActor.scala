package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase._
import com.tooe.core.migration.db.domain.MappingCollection
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.usecase.location_photoalbum.LocationPhotoAlbumDataActor
import com.tooe.core.usecase.location_photo.{LocationPhotoWriteActor, LocationPhotoDataActor}
import com.tooe.core.usecase.location.LocationDataActor
import com.tooe.core.domain._
import com.tooe.core.migration.PhotoMigratorActor.LegacyPhotoLike
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.domain.LocationId
import com.tooe.core.migration.PhotoMigratorActor.LegacyPhoto
import com.tooe.core.domain.UserId
import com.tooe.core.migration.api.MigrationResponse
import com.tooe.core.db.mongo.domain.LocationPhoto
import com.tooe.core.migration.api.DefaultMigrationResult
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.LocationsChainId
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.domain.LocationPhotoId

object LocationPhotoMigratorActor {
  val Id = 'locationPhotoMigratorActor

  import PhotoMigratorActor._
  case class LegacyLocationPhotoAlbum(legacyid: Int, locationid: Int, name: String,
                                      description: Option[String], photos: Seq[LegacyPhoto]) extends UnmarshallerEntity
}

class LocationPhotoMigratorActor extends MigrationActor {

  import LocationPhotoMigratorActor._

  def receive = {
    case lla: LegacyLocationPhotoAlbum =>
      import lla._
      val future =
        for {
          locId <- lookupByLegacyId(lla.locationid, MappingCollection.location).map(LocationId)
          chainId <- getLocationsChainId(locId)
          paId <- createPhotoAlbum(lla, locId, chainId)
          _ <- photosWrite(photos, paId, locId)
        } yield {
          MigrationResponse(DefaultMigrationResult(lla.legacyid, paId.id, "locationphoto_migrator"))
        }
      future pipeTo sender
  }

  def createPhotoAlbum(llpa: LegacyLocationPhotoAlbum, lid: LocationId, chain: Option[LocationsChainId]): Future[LocationPhotoAlbumId] = {
    import llpa._
    val photoAlbum = LocationPhotoAlbum(
      id = LocationPhotoAlbumId(),
      name = name,
      description = description,
      locationId = lid,
      photosCount = 0,
      frontPhotoUrl = MediaObject(MediaObjectId(photos.head.url), UrlType.MigrationType),
      locationsChainId = chain)

    photos.headOption.foreach {
      lp => saveUrl(EntityType.locationPhotoAlbum, photoAlbum.id.id, lp.url, UrlField.LocationPhotoAlbumMain)
    }

    updateStatisticsActor ! UpdateStatisticActor.ChangeLocationPhotoAlbumsCounter(lid, 1)
    (locationPhotoAlbumDataActor ? LocationPhotoAlbumDataActor.AddLocationPhotoAlbum(photoAlbum)).mapTo[LocationPhotoAlbumId]
  }

  def photosWrite(lps: Seq[LegacyPhoto], albumId: LocationPhotoAlbumId, lid: LocationId): Future[Seq[LocationPhotoId]] = {
    def photoWrite(photo: LegacyPhoto, albumId: LocationPhotoAlbumId, lid: LocationId): Future[LocationPhotoId] = {
      val photoId = LocationPhotoId(new ObjectId)
      saveUrl(EntityType.locationPhoto, photoId.id, photo.url, UrlField.LocationPhoto)
      locationDataActor ! LocationDataActor.AddPhotoToLocation(lid, photoId)
      locationPhotoAlbumDataActor ! LocationPhotoAlbumDataActor.UpdatePhotosCount(albumId, 1)
      (locationPhotoDataActor ? LocationPhotoDataActor.SaveLocationPhoto(LocationPhoto(id = photoId, photoAlbumId = albumId, locationId = lid, name = photo.name, fileUrl = MediaObject(MediaObjectId(photo.url), UrlType.MigrationType), creationDate = photo.time))).map {
        case lp: LocationPhoto => lp.id
      }
    }
    def photoLikesWrite(likes: Seq[LegacyPhotoLike], lpid: LocationPhotoId): Future[Int] =
      getIdMappings(likes.map(_.userid), MappingCollection.user).mapInner(uid =>
        locationPhotoWriteActor ? LocationPhotoWriteActor.LikeLocationPhoto(lpid, UserId(uid))
      ).map(_.length)

    Future.traverse[LegacyPhoto, LocationPhotoId, Seq](lps) {
      lp =>
        for {
          lpid <- photoWrite(lp, albumId, lid)
          likes <- photoLikesWrite(lp.likes, lpid)
        } yield lpid
    }
  }

  def getLocationsChainId(locationId: LocationId): Future[Option[LocationsChainId]] = {
    (locationDataActor ? LocationDataActor.GetLocation(locationId)).mapTo[Location]
      .map(loc => loc.locationsChainId)
  }

  lazy val updateStatisticsActor = lookup(UpdateStatisticActor.Id)
  lazy val locationPhotoAlbumDataActor = lookup(LocationPhotoAlbumDataActor.Id)
  lazy val locationPhotoDataActor = lookup(LocationPhotoDataActor.Id)
  lazy val locationPhotoWriteActor = lookup(LocationPhotoWriteActor.Id)
  lazy val locationDataActor = lookup(LocationDataActor.Id)
  lazy val dictionaryIdMapping = lookup(DictionaryIdMappingActor.Id)
}