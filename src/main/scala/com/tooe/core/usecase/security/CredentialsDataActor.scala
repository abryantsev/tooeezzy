package com.tooe.core.usecase.security

import com.tooe.core.application.Actors
import akka.actor.Actor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.service.CredentialsDataService
import scala.concurrent.Future
import akka.pattern.pipe
import com.tooe.core.exceptions.{NotFoundException, ApplicationException}
import com.tooe.core.util.HashHelper._
import com.tooe.core.db.mongo.domain.Credentials
import com.tooe.core.domain.{UserId, VerificationKey, CredentialsId}
import com.tooe.api.service.PasswordChangeRequest
import com.tooe.core.util.HashHelper

object CredentialsDataActor {
  final val Id = Actors.CredentialsData

  case class GetCredentials(login: String, password: String)
  case class Save(entity: Credentials)
  case class Remove(id: CredentialsId)
  case class FindByVerificationKey(vk: VerificationKey)
  case class ChangeUserPassword(userId: UserId, request: PasswordChangeRequest)
  case class ReplacePassword(login: String)
  case class ReplaceLegacyPassword(login: String, newPwdHash: String)
  case class GetCredentialsByLogin(login: String)
  case class GetCredentialsByUserIds(userIds: Seq[UserId])
}

class CredentialsDataActor extends Actor {

  lazy val service = BeanLookup[CredentialsDataService]

  import CredentialsDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case Save(credentials) => Future{ service.save(credentials) } pipeTo sender

    case Remove(credentialsId) => service.delete(credentialsId)
    case GetCredentials(login, password) => Future {
      service.find(userName = login, passwordHash = passwordHash(password)) getOrElse (throw
        ApplicationException(message = "User password verification has failed, login or password or both incorrect")
      )
    } pipeTo sender

    case FindByVerificationKey(vk) => Future { service.find(vk) } pipeTo sender

    case ChangeUserPassword(userId, request) => Future { service.updateUserPassword(userId, request) } pipeTo sender

    case ReplaceLegacyPassword(login, newPwdHash) => Future(service.replaceLegacyPassword(login, newPwdHash))

    case ReplacePassword(login) => Future{
      val password = HashHelper.uuid
      service.changePassword(login, HashHelper.passwordHash(password))
      password
    } pipeTo sender

    case GetCredentialsByLogin(login) =>
      Future(service.findByLogin(login)) map {
        _ getOrElse (throw NotFoundException(s"Not found credentials by login: $login"))
      } pipeTo sender
    
    case GetCredentialsByUserIds(userIds) =>
      Future(service.findByUserIds(userIds)) pipeTo sender
  }

}