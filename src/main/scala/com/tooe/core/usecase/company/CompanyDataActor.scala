package com.tooe.core.usecase.company

import com.tooe.core.application.Actors
import com.tooe.core.usecase._
import com.tooe.core.domain.{AdminUserId, CompanyId}
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CompanyDataService
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.Company
import com.tooe.api.service.{SearchCompanyRequest, OffsetLimit, CompanyExportedRequest}
import com.tooe.core.usecase.job.urls_check.ChangeUrlType

object CompanyDataActor {
  final val Id = Actors.CompanyDataActor

  case class GetCompany(id: CompanyId)
  case class GetCompanies(ids: Seq[CompanyId])
  case class SaveCompany(company: Company)
  case class GetExportedCompanies()
  case class ExportedCompaniesCount()
  case class CompaniesExported(request: CompanyExportedRequest)
  case class SearchCompany(request: SearchCompanyRequest, offsetLimit: OffsetLimit)
  case class SearchCompanyCount(request: SearchCompanyRequest)
  case class FindCompaniesByAgentUserId(agentId: AdminUserId)
  case class GetCompanyMedia(id: CompanyId)
}

class CompanyDataActor extends AppActor {

  import CompanyDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val service = BeanLookup[CompanyDataService]

  def receive = {

    case GetCompany(id) => Future { service.find(id) getOrNotFound(id.id, "Company not found") } pipeTo sender

    case GetCompanies(ids) => Future { service.findAllByIds(ids) } pipeTo sender

    case SaveCompany(company) => Future { service.save(company) } pipeTo sender

    case GetExportedCompanies => Future { service.getExportedCompanies() } pipeTo sender

    case ExportedCompaniesCount => Future { service.exportedCompaniesCount() } pipeTo sender

    case CompaniesExported(request) => Future { service.exportedCompaniesComplete(request.ids) }

    case SearchCompany(request, offsetLimit) => Future { service.search(request, offsetLimit) } pipeTo sender

    case SearchCompanyCount(request) => Future { service.searchCount(request) } pipeTo sender

    case FindCompaniesByAgentUserId(agentId) =>
      Future(service.findCompaniesByAgentUserId(agentId) map (_.id)) pipeTo sender

    case GetCompanyMedia(id) => Future { service.getCompanyMedia(id) } pipeTo sender

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(CompanyId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(CompanyId(msg.url.entityId), msg.url.mediaId) }
  }

}
