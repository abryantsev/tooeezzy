package com.tooe.core.usecase.location_photoalbum

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.api.boot.DefaultTimeout
import akka.actor.Actor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.LocationPhotoAlbumDataService
import scala.concurrent.Future
import com.tooe.core.domain._
import akka.pattern.pipe
import com.tooe.core.usecase.OptionWrapper
import com.tooe.api.service.OffsetLimit
import com.tooe.core.domain.LocationId
import com.tooe.core.db.mongo.domain.LocationPhotoAlbum
import com.tooe.core.domain.LocationsChainId
import com.tooe.core.domain.LocationPhotoAlbumId
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object LocationPhotoAlbumDataActor {

  final val Id = Actors.LocationPhotoAlbumData

  case class AddLocationPhotoAlbum(album: LocationPhotoAlbum)
  case class FindLocationPhotoAlbum(albumId: LocationPhotoAlbumId)
  case class CountByLocation(locationId: LocationId)
  case class LocationPhotoAlbums(locationId: LocationId, offsetLimit: OffsetLimit)
  case class UpdatePhotosCount(photoId: LocationPhotoAlbumId, delta: Int)
  case class DeleteLocationPhotoAlbum(albumId: LocationPhotoAlbumId)
  case class UpdateLocationPhotoAlbum(albumId: LocationPhotoAlbumId, request: UpdateLocationPhotoAlbumRequest)
  case class FindLocationsChainPhotoAlbums(locationsChainId: LocationsChainId, offsetLimit: OffsetLimit)
  case class CountLocationsChainPhotoAlbums(locationsChainId: LocationsChainId)

}

class LocationPhotoAlbumDataActor extends Actor with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import LocationPhotoAlbumDataActor._

  lazy val service = BeanLookup[LocationPhotoAlbumDataService]

  def receive = {

    case AddLocationPhotoAlbum(album) =>
      Future {
        val entity = service.save(album)
        entity.id
      } pipeTo sender

    case FindLocationPhotoAlbum(albumId) => Future { service.findOne(albumId).getOrNotFound(albumId.id, "location_photoalbum") } pipeTo sender

    case CountByLocation(locationId) => Future { service.countByLocation(locationId) } pipeTo sender

    case LocationPhotoAlbums(locationId, offsetLimit) => Future { service.albumsByLocation(locationId, offsetLimit)  } pipeTo sender

    case UpdatePhotosCount(photoId, delta) => Future { service.updatePhotosCount(photoId, delta) } pipeTo sender

    case DeleteLocationPhotoAlbum(albumId) => Future { service.delete(albumId)  }

    case UpdateLocationPhotoAlbum(albumId, request) => Future { service.update(albumId, request) }

    case FindLocationsChainPhotoAlbums(locationsChainId, offsetLimit) =>
     Future { service.findChainsAlbums(locationsChainId, offsetLimit)  } pipeTo sender

    case CountLocationsChainPhotoAlbums(locationsChainId) => Future { service.countChainsAlbums(locationsChainId)  } pipeTo sender

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(LocationPhotoAlbumId(msg.url.entityId), msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(LocationPhotoAlbumId(msg.url.entityId)) }

  }

}

case class UpdateLocationPhotoAlbumRequest(name: Option[String],
                                           description: Unsetable[String],
                                           photoUrl: Option[MediaObject])