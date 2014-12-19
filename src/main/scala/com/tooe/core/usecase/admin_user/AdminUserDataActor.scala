package com.tooe.core.usecase.admin_user

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import akka.pattern._
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.db.mongo.domain.AdminUser
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.AdminUserDataService
import scala.concurrent.Future
import com.tooe.core.domain.{AdminRoleId, CompanyId, AdminUserId}
import com.tooe.core.usecase.OptionWrapper
import com.tooe.api.service.{OffsetLimit, SearchAdminUserRequest, AdminUserChangeRequest}

object AdminUserDataActor {
  final val Id = Actors.AdminUserData

  case class SaveAdminUser(adminUser: AdminUser)
  case class FindAdminUser(adminUserId: AdminUserId)
  case class FindByRolesForCompany(companyId: CompanyId, roles: Seq[AdminRoleId])
  case class ChangeAdminUser(adminUserId: AdminUserId, request: AdminUserChangeRequest)
  case class SearchAdminUsers(request: SearchAdminUserRequest, offsetLimit: OffsetLimit)
  case class CountAdminUsers(request: SearchAdminUserRequest)
  case class ActivateAdminUser(adminUserId: AdminUserId, companyId: CompanyId)
  case class DeleteAdminUser(adminUserId: AdminUserId)
  case class GetAdminUsers(ids: Seq[AdminUserId])
  case class FindAdminUsersByCompanyAndRoles(companyIds: Seq[CompanyId], roles: Seq[AdminRoleId])
}

class AdminUserDataActor extends Actor with AppActors with DefaultTimeout  {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserDataActor._

  lazy val service = BeanLookup[AdminUserDataService]

  def receive = {

    case SaveAdminUser(adminUser) => Future { service.save(adminUser) } pipeTo sender
    case FindAdminUser(adminUserId) => Future { service.findActiveUser(adminUserId).getOrNotFound(adminUserId, "Admin user not found")} pipeTo sender
    case ChangeAdminUser(adminUserId, request) => Future { service.change(adminUserId, request) }
    case SearchAdminUsers(request, offsetLimit) => Future { service.search(request, offsetLimit) } pipeTo sender
    case CountAdminUsers(request) => Future { service.count(request) } pipeTo sender
    case ActivateAdminUser(adminUserId, companyId) => Future { service.activateUser(adminUserId, companyId) }
    case DeleteAdminUser(adminUserId) => Future { service.delete(adminUserId) }
    case GetAdminUsers(ids) => Future { service.find(ids) } pipeTo sender
    case FindByRolesForCompany(id, roles) => Future(service.findByRolesForCompany(id, roles)).pipeTo(sender)
    case FindAdminUsersByCompanyAndRoles(companyIds, roles) => Future { service.findAdminUsersByCompanyAndRoles(companyIds, roles) } pipeTo sender
  }

}