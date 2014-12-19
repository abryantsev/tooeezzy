package com.tooe.core.usecase.admin_user

import com.tooe.core.application.Actors
import com.tooe.api.service.{ExecutionContextProvider, OffsetLimit, SearchAdminUserRequest, SuccessfulResponse}
import com.tooe.core.domain.{AdminRoleId, AdminUserId}
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Date
import com.tooe.core.db.mongo.domain.{AdminCredentials, AdminUser}
import scala.concurrent.Future
import com.tooe.core.usecase.admin_user.AdminUserDataActor.FindAdminUser
import com.tooe.core.usecase.admin_user.AdminCredentialsDataActor.GetAdminUserCredentials
import com.tooe.core.usecase.{GetAllLifecycleStatusesResponse, LifecycleStatusActor, GetAllLifecycleStatusesResponseItem, AppActor}
import com.tooe.core.util.Lang

object AdminUserReadActor {
  final val Id = Actors.AdminUserRead

  case class SearchAdminUser(request: SearchAdminUserRequest, offsetLimit: OffsetLimit, lang: Lang)

  case class GetAdminUserInfo(id: AdminUserId)

}


class AdminUserReadActor extends AppActor with ExecutionContextProvider {

  import AdminUserReadActor._

  lazy val adminUserDateActor = lookup(AdminUserDataActor.Id)
  lazy val adminCredentialsDataActor = lookup(AdminCredentialsDataActor.Id)
  lazy val lifecycleStatusActor = lookup(LifecycleStatusActor.Id)

  def receive = {
    case SearchAdminUser(request, offsetLimit, lang) =>
      (for {
        count <- countAdminUsers(request)
        users <- searchAdminUsers(request, offsetLimit)
        credentials <- getUserCredentials(users)
        credentialsMap = credentials.map(c => (c.adminUserId, c.userName)).toMap
        lifeStatuses <- (lifecycleStatusActor ?  LifecycleStatusActor.GetAllLifecycleStatuses(lang)).mapTo[GetAllLifecycleStatusesResponse]
        lifeStatusesMap = lifeStatuses.lifecyclestatuses.toMapId(_.id)
      } yield {
        AdminUserSearchResult(count, users.map(u => AdminUserItem(u, credentialsMap, lifeStatusesMap)))
      }) pipeTo sender

    case GetAdminUserInfo(id) =>
      getAdminUser(id).zip(getAdminCredentials(id)).map {
        case (admin, creds) => GetAdminUserInfoResponse(AdminUserInfo(admin, creds))
      }.pipeTo(sender)

  }

  def getAdminUser(id: AdminUserId) =
    adminUserDateActor.ask(FindAdminUser(id)).mapTo[AdminUser]

  def getAdminCredentials(id: AdminUserId) =
    adminCredentialsDataActor.ask(GetAdminUserCredentials(id)).mapTo[AdminCredentials]

  def countAdminUsers(request: SearchAdminUserRequest) =
    adminUserDateActor.ask(AdminUserDataActor.CountAdminUsers(request)).mapTo[Long]

  def searchAdminUsers(request: SearchAdminUserRequest, offsetLimit: OffsetLimit) =
    adminUserDateActor.ask(AdminUserDataActor.SearchAdminUsers(request, offsetLimit)).mapTo[Seq[AdminUser]]

  def getUserCredentials(users: Seq[AdminUser]): Future[Seq[AdminCredentials]] = {
    (adminCredentialsDataActor ? AdminCredentialsDataActor.GetAdminCredentials(users.map(_.id))).mapTo[Seq[AdminCredentials]]
  }
}

case class AdminUserItem(id: AdminUserId,
                         name: String,
                         @JsonProperty("lastname") lastName: String,
                         username: Option[String],
                         @JsonProperty("regdate") registerDate: Date,
                         role: AdminRoleId,
                         status: Option[GetAllLifecycleStatusesResponseItem])

object AdminUserItem {

  def apply(user: AdminUser, emails: Map[AdminUserId, String], statuses: Map[String, GetAllLifecycleStatusesResponseItem]): AdminUserItem =
    AdminUserItem(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      username = emails.get(user.id),
      registerDate = user.registrationDate,
      role = user.role,
      status = user.lifecycleStatus.flatMap(status => statuses.get(status.id))
    )

}

case class GetAdminUserInfoResponse(admuser: AdminUserInfo) extends SuccessfulResponse

case class AdminUserInfo(name: String, lastname: String, username: String, role: String)

object AdminUserInfo {
  def apply(admin: AdminUser, creds: AdminCredentials): AdminUserInfo =
    AdminUserInfo(
      name = admin.name,
      lastname = admin.lastName,
      username = creds.userName,
      role = admin.role.id
    )
}

case class AdminUserSearchResult(@JsonProperty("admuserscount") count: Long, @JsonProperty("admusers") users: Seq[AdminUserItem]) extends SuccessfulResponse