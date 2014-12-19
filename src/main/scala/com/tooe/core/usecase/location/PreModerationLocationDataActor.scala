package com.tooe.core.usecase.location

import com.tooe.core.domain._
import com.tooe.core.application.Actors
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.{PreModerationLocationAdminSearchParams, PreModerationLocationDataService}
import scala.concurrent.Future
import com.tooe.core.usecase._
import com.tooe.core.usecase.job.urls_check.ChangeUrlType
import com.tooe.core.util.Lang
import com.tooe.core.usecase._
import com.tooe.api.service._
import com.tooe.core.db.mongo.domain.PreModerationLocation

object PreModerationLocationDataActor {
  final val Id = Actors.PreModerationLocationData

  case class FindLocationById(id: PreModerationLocationId)

  case class FindLocationByLocationId(locationId: LocationId)

  case class ModerationLocationSearch(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang)

  case class CountModerationLocationSearch(request: ModerationLocationsSearchRequest, companies: Set[CompanyId], lang: Lang)

  case class FindLocationsByAdminSearchParams(params: PreModerationLocationAdminSearchParams)

  case class CountLocationsByAdminSearchParams(params: PreModerationLocationAdminSearchParams)

  case class SaveLocation(pml: PreModerationLocation)

  case class UpdateLocation(locationId: PreModerationLocationId, ulr: UpdateLocationRequest, ctx: RouteContext)

  case class GetPublishLocationId(id: PreModerationLocationId)

  case class UpdatePublishId(id: PreModerationLocationId, locationId: LocationId)

  case class UpdateModerationStatus(id: PreModerationLocationId, request: LocationModerationRequest, userId: AdminUserId)

  case class UpdateLifecycleStatus(id: PreModerationLocationId, status: Option[LifecycleStatusId])

}

class PreModerationLocationDataActor extends AppActor {

  import PreModerationLocationDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PreModerationLocationDataService]

  def receive = {
    case SaveLocation(pml) => Future(service.save(pml)).pipeTo(sender)
    case UpdateLocation(locationId, ulr, ctx) => Future { service.updateLocation(locationId, ulr, ctx.lang) }
    case GetPublishLocationId(id) => Future { service.getPublishLocationId(id) } pipeTo sender
    case UpdatePublishId(id, locationId) => Future { service.updatePublishId(id, locationId) }
    case UpdateModerationStatus(id, request, userId) => Future { service.updateModerationStatus(id, request, userId) }
    case FindLocationById(id) =>
      Future(service.findById(id).getOrNotFound(id.id, "pre moderation location")).pipeTo(sender)
    case FindLocationByLocationId(locationId) =>
      Future(service.findByLocationId(locationId).getOrNotFoundException(s"pre moderation location does not exist for location ${locationId.id}")).pipeTo(sender)
    case FindLocationsByAdminSearchParams(params) => Future(service.findByAdminSearchParams(params)).pipeTo(sender)
    case CountLocationsByAdminSearchParams(params) => Future(service.countByAdminSearchParams(params)).pipeTo(sender)

    case ModerationLocationSearch(request, companies, lang) => Future(service.findOwnModerationLocations(request, companies, lang)).pipeTo(sender)
    case CountModerationLocationSearch(request, companies, lang) => Future(service.countOwnModerationLocations(request, companies, lang)).pipeTo(sender)
    case UpdateLifecycleStatus(id, status) => Future(service.updateLifecycleStatus(id, status)).pipeTo(sender)

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(PreModerationLocationId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(PreModerationLocationId(msg.url.entityId), msg.url.mediaId) }
  }
}
