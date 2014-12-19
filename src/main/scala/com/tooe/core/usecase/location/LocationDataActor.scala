package com.tooe.core.usecase.location

import akka.actor.Actor
import com.tooe.core.util.{Lang, InfoMessageHelper, ActorHelper}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.application.{Actors, AppActors}
import scala.concurrent.Future
import akka.pattern.pipe
import com.tooe.core.service.LocationDataService
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.exceptions.{ApplicationException, NotFoundException}
import com.tooe.core.domain._
import com.tooe.core.db.mongo.query.UpdateResult.NoUpdate
import com.tooe.core.usecase._
import com.tooe.core.usecase.ChangeAdditionalCategoryRequest
import com.tooe.core.usecase.RenameAdditionalCategoryParameters
import com.tooe.core.db.mongo.domain.{PreModerationLocation, AdditionalLocationCategory, Location}
import com.tooe.core.usecase.LocationsSearchRequest
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.UserId
import com.tooe.api.service.RouteContext
import com.tooe.core.domain.AdditionalLocationCategoryId
import com.tooe.core.domain.LocationPhotoId
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object LocationDataActor {
  final val Id = Actors.LocationData

  case class GetLocationsSearchByCoordinates(request: LocationsSearchRequest, lang: Lang)
  case class GetFavoriteLocationsBy(request: GetFavoriteLocationsRequest, ids: Seq[LocationId], lang: Lang)
  case class GetLocation(id: LocationId)
  case class GetLocationWithAnyLifeCycleStatus(id: LocationId)
  case class GetActiveLocationOrByLifeCycleStatuses(id: LocationId, statuses: Seq[LifecycleStatusId])
  case class GetProductCategories(id: LocationId)
  case class SaveLocation(location: Location)
  case class RemoveLocation(locationId: LocationId)
  case class AddOwnProductCategory(locationId: LocationId, categoryName: String, ctx: RouteContext)
  case class ChangeAdditionalCategory(request: ChangeAdditionalCategoryRequest, renameParameters: RenameAdditionalCategoryParameters, ctx: RouteContext)
  case class RemoveOwnProductCategory(request: ChangeAdditionalCategoryRequest)
  case class AddProductCategory(productCategory: AdditionalLocationCategory, ctx: RouteContext)
  case class FindLocations(ids: Set[LocationId])
  case class AddPhotoToLocation(locationId: LocationId, photoId: LocationPhotoId)
  case class UpdateLocationPhotos(locationId: LocationId, photoIds: Seq[LocationPhotoId])
  case class GetStatistics(locationId: LocationId)
  case class UpdateStatistic(locationId: LocationId, updater: UpdateLocationStatistic)
  case class PutUserToUsersWhoFavorite(locationId: LocationId, userId: UserId)
  case class RemoveUserFromUsersWhoFavorite(locationId: LocationId, userId: UserId)
  case class GetLocationsForInvitations(request: GetLocationsForInvitationRequest, lang: Lang)
  case class GetLocationsForCheckin(request: GetLocationsForCheckinRequest, lang: Lang)
  case class GetLocationsByChain(request: GetLocationsByChainRequest, lang: Lang)
  case class UpdatePromotionsFlag(lid: LocationId, flag: Option[Boolean])
  case class AddLocationsToChain(id: LocationsChainId, locations: Seq[LocationId])
  case class UpdateLocation(id: LocationId, preModerationLocation: PreModerationLocation)
  case class GetMedia(id: LocationId)
  case class LocationExistsCheck(id: LocationId)
  case class UpdateLifecycleStatus(id: LocationId, status: Option[LifecycleStatusId])
  case class AddPhotoAlbumToLocation(loc: LocationId, album: LocationPhotoAlbumId)
  case class DeletePhotoAlbumFromLocation(loc: LocationId, album: LocationPhotoAlbumId)

}

class LocationDataActor extends Actor with ActorHelper with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[LocationDataService]
  lazy val updateStatisticsActor = lookup(UpdateStatisticActor.Id)

  import LocationDataActor._

  def receive = {
    case AddLocationsToChain(id, locations) => Future(service.addLocationsToChain(id, locations)) pipeTo sender

    case GetLocationsForCheckin(request, lang) => Future{ service.getLocationsForCheckin(request, lang)} pipeTo sender

    case GetLocationsByChain(request, lang) => Future{ service.getLocationsByChain(request, lang)} pipeTo sender

    case GetLocationsForInvitations(request, lang) => Future{ service.getLocationsForInvitation(request, lang)} pipeTo sender

    case PutUserToUsersWhoFavorite(locationId, userId) => Future {
      service.putUserToUsersWhoFavorite(locationId, userId)
    }

    case RemoveUserFromUsersWhoFavorite(locationId, userId) => Future {
      service.removeUserFromUsersWhoFavorite(locationId, userId)
    }

    case SaveLocation(location) => Future {
      import location.contact.address._
      updateStatisticsActor ! UpdateStatisticActor.AddLocationCategories(regionId, location.locationCategories, countryId)
      service.save(location)
    } pipeTo sender

    case RemoveLocation(locationId) =>
      Future {
        service.findOne(locationId)
          .getOrElse(throw NotFoundException(s"Location with id ${locationId.id} not found"))
      }.map {
        location =>
          import location.contact.address._
          updateStatisticsActor ! UpdateStatisticActor.RemoveLocationCategories(regionId, location.locationCategories, countryId)
          service.deleteLocation(locationId)
      } pipeTo sender

    case AddOwnProductCategory(locationId, categoryName, ctx) =>
      implicit val lang = ctx.lang
      val newCategoryId = AdditionalLocationCategoryId()
      Future {
        service.addOwnCategory(locationId, newCategoryId, categoryName)
      } flatMap {
        case NoUpdate => InfoMessageHelper.throwAppExceptionById("duplicate_location_own_category_id")
        case _ => Future {
          newCategoryId
        }
      } pipeTo sender

    case RemoveOwnProductCategory(request) =>
      Future {
        service.removeAdditionalCategory(request.locationId, request.categoryId)
      } pipeTo sender

    case ChangeAdditionalCategory(request, renameParameters, ctx) =>
      Future {
        service.renameAdditionalCategory(request, renameParameters, ctx.lang)
      } pipeTo sender

    case GetFavoriteLocationsBy(request, locationIds, lang) => {
      Future {
        service.getFavoriteLocations(request, locationIds, lang)
      } pipeTo sender
    }

    case GetLocation(locationId) => Future {
      service.findOne(locationId).getOrElse (throw NotFoundException(locationId.toString + " not found"))
    } pipeTo sender

    case AddPhotoAlbumToLocation(loc, album) => Future {
      service.addPhotoAlbumToLocation(loc, album)
    }

    case DeletePhotoAlbumFromLocation(loc, album) => Future {
      service.deletePhotoAlbumFromLocation(loc, album)
    }

    case GetLocationWithAnyLifeCycleStatus(locationId) => Future {
      service.getLocationForAdmin(locationId) getOrElse (throw NotFoundException(locationId.toString + " not found"))
    } pipeTo sender

    case GetActiveLocationOrByLifeCycleStatuses(locationId, statuses) => Future {
      service.getActiveLocationOrByLifeCycleStatuses(locationId, statuses) getOrElse (throw NotFoundException(locationId.toString + " not found"))
    } pipeTo sender

    case GetLocationsSearchByCoordinates(request, lang) =>
      Future(service.getLocationsSearchByCoordinates(request, lang)) pipeTo sender

    case GetProductCategories(locationId) =>
      Future {
        service.findOne(locationId).map(_.additionalLocationCategories)
          .getOrElse(throw NotFoundException(s"Location with id ${locationId.id} not found"))
      } pipeTo sender

    case FindLocations(ids) => Future(service.getLocations(ids.toSeq)) pipeTo sender

    case AddPhotoToLocation(locationId, photoId) => Future {
      service.addPhotoToLocation(locationId, photoId)
    }

    case UpdateLocationPhotos(locationId, photoIds) => Future {
      service.updateLocationPhotos(locationId, photoIds)
    }

    case GetStatistics(locationId) => Future {
      service.getStatistics(locationId)
        .getOrElse(throw NotFoundException(s"Location with id ${locationId.id} not found"))
    } pipeTo sender
    case UpdateStatistic(locationId, updater) =>
      Future {
        service.changeStatistic(locationId, updater)
      } pipeTo sender

    case UpdatePromotionsFlag(lid, flag) => {
      Future {
        service.setPromotionsFlag(lid, flag)
      }
    }

    case LocationExistsCheck(id) =>
      Future {
        service.isLocationExist(id) match {
          case true => true
          case false => throw ApplicationException(message = "No such location", errorCode = 400)
        }
      } pipeTo sender

    case UpdateLocation(id, preModerationLocation) =>
      Future {
        val oldLocation = service.findOne(id).getOrElse(throw NotFoundException(s"Location with id ${id.id} not found"))
        if (oldLocation.contact.address.regionId == preModerationLocation.contact.address.regionId) {
          val oldCategories = oldLocation.locationCategories
          val newCategories = preModerationLocation.locationCategories
          if (oldCategories != newCategories) {
            val (added, removed) = (newCategories.filterNot(oldCategories.contains), oldCategories.filterNot(newCategories.contains))
            updateStatisticsActor ! UpdateStatisticActor.AddLocationCategories(oldLocation.contact.address.regionId, added, oldLocation.contact.address.countryId)
            updateStatisticsActor ! UpdateStatisticActor.RemoveLocationCategories(oldLocation.contact.address.regionId, removed, oldLocation.contact.address.countryId)
          }
        } else {
          updateStatisticsActor ! UpdateStatisticActor.AddLocationCategories(preModerationLocation.contact.address.regionId, preModerationLocation.locationCategories, preModerationLocation.contact.address.countryId)
          updateStatisticsActor ! UpdateStatisticActor.RemoveLocationCategories(oldLocation.contact.address.regionId, oldLocation.locationCategories, oldLocation.contact.address.countryId)
        }
        service.updateLocation(id, preModerationLocation)
      }

    case UpdateLifecycleStatus(id, status) => Future(service.updateLifecycleStatus(id, status)).pipeTo(sender)

    case GetMedia(id) => Future { service.getMedia(id) } pipeTo sender

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(LocationId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(LocationId(msg.url.entityId), msg.url.mediaId) }

  }
}