package com.tooe.core.usecase.admin_user

import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import akka.pattern._
import com.tooe.api.boot.DefaultTimeout
import com.tooe.api.service.{AdminUserChangeRequest, SuccessfulResponse, AdminUserRegisterRequest}
import com.tooe.core.usecase.admin_user.AdminUserDataActor.SaveAdminUser
import com.tooe.core.db.mongo.domain.{AdminCredentials, AdminUser}
import com.tooe.core.domain.AdminUserId
import com.tooe.core.util.HashHelper._
import com.tooe.core.usecase.admin_user.AdminCredentialsDataActor.SaveAdminCredentials
import com.tooe.core.usecase.admin_user_event.AdminUserEventDataActor
import com.tooe.core.exceptions.ApplicationException

object AdminUserWriteActor {
  final val Id = Actors.AdminUserWrite

  case class CreateAdminUser(request: AdminUserRegisterRequest)
  case class ChangeAdminUser(adminUserId: AdminUserId, request: AdminUserChangeRequest)
  case class DeleteAdminUser(adminUserId: AdminUserId)
}

class AdminUserWriteActor extends Actor with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminUserWriteActor._

  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val adminCredentialsDataActor = lookup(AdminCredentialsDataActor.Id)
  lazy val adminUserEventDataActor = lookup(AdminUserEventDataActor.Id)

  def receive = {

    case CreateAdminUser(request) =>
      (for {
        _ <- isEmailAlreadyExists(request.email).map(p => if (p) throw new ApplicationException(message = "Admin user with this e-mail already exists.") else p)
        adminId = AdminUserId()
        credentials <- (adminCredentialsDataActor ? SaveAdminCredentials(requestToAdminCredentials(request, adminId))).mapTo[AdminCredentials]
        adminUser <- (adminUserDataActor ? SaveAdminUser(requestToAdminUser(request).copy(id = adminId))).mapTo[AdminUser]
      } yield {

        AdminUserCreatedResponse(AdminUserIdCreated(adminUser.id))
      }) pipeTo sender

    case ChangeAdminUser(adminUserId, request) =>
      adminUserDataActor ! AdminUserDataActor.ChangeAdminUser(adminUserId, request)
      adminCredentialsDataActor ! AdminCredentialsDataActor.ChangeAdminCredentials(adminUserId, request)
      sender ! SuccessfulResponse

    case DeleteAdminUser(adminUserId) =>
      adminUserDataActor ! AdminUserDataActor.DeleteAdminUser(adminUserId)
      adminCredentialsDataActor ! AdminCredentialsDataActor.DeleteAdminCredentials(adminUserId)
      adminUserEventDataActor ! AdminUserEventDataActor.DeleteAdminEvenByUser(adminUserId)
      sender ! SuccessfulResponse

  }

  def requestToAdminUser(request: AdminUserRegisterRequest) =
    AdminUser(name = request.name, lastName = request.lastName, role = request.role, description = request.description)

  def requestToAdminCredentials(request: AdminUserRegisterRequest, adminUserId: AdminUserId) =
    AdminCredentials(adminUserId = adminUserId, userName = request.email, password = passwordHash(request.password))

  def isEmailAlreadyExists(email: String) =
    (adminCredentialsDataActor ? AdminCredentialsDataActor.AdminUserEmailExist(email)).mapTo[Boolean]

}

case class AdminUserCreatedResponse(admuser: AdminUserIdCreated) extends SuccessfulResponse

case class AdminUserIdCreated(id: AdminUserId)