package com.tooe.core.usecase.company

import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.PreModerationCompanyDataService
import scala.concurrent.Future
import com.tooe.core.application.Actors
import com.tooe.core.db.mongo.domain.{CompanyMedia, PreModerationCompany}
import com.tooe.core.domain.{MediaObjectId, CompanyId, AdminUserId, PreModerationCompanyId}
import com.tooe.api.service.{SearchModerationCompanyRequest, CompanyChangeRequest, OffsetLimit, CompanyModerationRequest}
import com.tooe.core.usecase._
import com.tooe.core.exceptions.NotFoundException
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object PreModerationCompanyDataActor {
  final val Id = Actors.PreModerationCompanyDataActor

  case class SaveCompany(company: PreModerationCompany)
  case class FindCompany(companyId: PreModerationCompanyId)
  case class GetPublishCompanyId(companyId: PreModerationCompanyId)
  case class UpdateStatus(companyId: PreModerationCompanyId, request: CompanyModerationRequest, userId: AdminUserId, publishId: Option[CompanyId])
  case class SearchCompany(request: SearchModerationCompanyRequest, offsetLimit: OffsetLimit)
  case class SearchCompanyCount(request: SearchModerationCompanyRequest)
  case class ChangeCompany(companyId: PreModerationCompanyId, request: CompanyChangeRequest)
  case class FindPreModerationCompanyByCompanyId(id: CompanyId)
  case class UpdateCompanyMedia(companyId: PreModerationCompanyId, media: CompanyMedia)
}

class PreModerationCompanyDataActor extends AppActor {

  import PreModerationCompanyDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[PreModerationCompanyDataService]

  def receive = {
    case SaveCompany(company) => Future { service.save(company) } pipeTo sender
    case FindCompany(id) => Future { service.find(id).getOrFailure(NotFoundException(s"PreModerationCompany(id=$id) does not exist")) } pipeTo sender
    case UpdateStatus(companyId, request, userId, publishId) => Future { service.updateStatus(companyId, request, userId, publishId) }
    case GetPublishCompanyId(companyId) => Future { service.getPublishCompanyId(companyId) } pipeTo sender
    case SearchCompany(request, offsetLimit) => Future { service.search(request, offsetLimit) } pipeTo sender
    case SearchCompanyCount(request) => Future { service.searchCount(request) } pipeTo sender
    case ChangeCompany(id, request) => Future { service.changeCompany(id, request) }.pipeTo(sender)
    case FindPreModerationCompanyByCompanyId(id) => Future { service.findByCompanyId(id).getOrFailure(NotFoundException(s"PreModerationCompany(publish id=$id) does not exist")) } pipeTo sender
    case UpdateCompanyMedia(id, media) => Future(service.updateCompanyMedia(id, media)).pipeTo(sender)

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(PreModerationCompanyId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(PreModerationCompanyId(msg.url.entityId), msg.url.mediaId) }
  }

}
