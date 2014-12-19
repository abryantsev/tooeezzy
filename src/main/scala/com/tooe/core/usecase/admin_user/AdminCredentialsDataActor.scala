package com.tooe.core.usecase.admin_user

import com.tooe.core.application.{AppActors, Actors}
import com.tooe.core.domain.{AdminUserId, AdminCredentialsId}
import akka.actor.Actor
import akka.pattern._
import com.tooe.core.db.mongo.domain.AdminCredentials
import com.tooe.api.boot.DefaultTimeout
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.AdminCredentialsDataService
import scala.concurrent.Future
import com.tooe.core.util.HashHelper._
import com.tooe.api.service.AdminUserChangeRequest
import com.tooe.core.usecase._
import com.tooe.core.exceptions.NotFoundException

object AdminCredentialsDataActor {
  final val Id = Actors.AdminCredentialsData

  case class GetCredentials(login: String, password: String)
  case class SaveAdminCredentials(adminCredentials: AdminCredentials)
  case class FindAdminCredentials(adminCredentialsId: AdminCredentialsId)
  case class ChangeAdminCredentials(adminUserId: AdminUserId, request: AdminUserChangeRequest)
  case class GetAdminCredentials(ids: Seq[AdminUserId])
  case class GetAdminUserCredentials(id: AdminUserId)
  case class DeleteAdminCredentials(id: AdminUserId)
  case class ReplaceLegacyPassword(login: String, newPwdHash: String)
  case class GetCredentialsByLogin(login: String)
  case class AdminUserEmailExist(email: String)
}

class AdminCredentialsDataActor extends Actor with AppActors with DefaultTimeout {

  import scala.concurrent.ExecutionContext.Implicits.global
  import AdminCredentialsDataActor._

  lazy val service = BeanLookup[AdminCredentialsDataService]

  def receive = {
    case SaveAdminCredentials(adminCredentials) => Future { service.save(adminCredentials) } pipeTo sender
    case FindAdminCredentials(adminCredentialsId) => Future { service.findOne(adminCredentialsId) } pipeTo sender
    case GetCredentials(login, password) => Future {
      service.find(username = login, passwordHash = passwordHash(password))
                .getOrNotFoundException("User password verification has failed, login or password or both incorrect")
    } pipeTo sender
    case ChangeAdminCredentials(adminUserId, request) => Future { service.change(adminUserId, request) }
    case GetAdminCredentials(ids) => Future { service.findByUserIds(ids) } pipeTo sender
    case GetAdminUserCredentials(id) => Future { service.findByUserId(id).getOrElse(throw NotFoundException(s"Not found admin credentials for user: ${id.id}")) } pipeTo sender
    case DeleteAdminCredentials(id) => Future { service.deleteByUserId(id) }
    case ReplaceLegacyPassword(login, newPwdHash) => Future(service.replaceLegacyPassword(login, newPwdHash))
    case GetCredentialsByLogin(login) =>
      Future(service.findByLogin(login)) map {
        _ getOrElse (throw NotFoundException(s"Not found admin credentials by login: $login"))
      } pipeTo sender
    case AdminUserEmailExist(email) => Future { service.emailExist(email) } pipeTo sender
  }
}