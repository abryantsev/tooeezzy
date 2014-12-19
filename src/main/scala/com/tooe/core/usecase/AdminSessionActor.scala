package com.tooe.core.usecase

import akka.pattern._
import com.tooe.core.domain._
import com.tooe.core.application.{AppActors, Actors}
import akka.actor.Actor
import com.tooe.core.util.{LegacyPasswordChecker, DateHelper, HashHelper, ActorHelper}
import com.tooe.api.boot.DefaultTimeout
import scala.concurrent.Future
import com.tooe.core.usecase.admin_user.{AdminUserDataActor, AdminCredentialsDataActor}
import com.tooe.core.db.mongo.domain.{AdminUser, AdminCredentials, CacheAdminSession}
import com.tooe.api.service.{AuthCookies, SuccessfulResponse}
import com.tooe.core.util.DateHelper._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.core.exceptions.NotFoundException

object AdminSessionActor {
  final val Id = Actors.AdminSession

  case class Login(login: String, password: String, token: Option[SessionToken])
  case class Authenticate(authCookies: AuthCookies)
  case class Logout(token: SessionToken)

  case class LoginResult(userId: AdminUserId, token: SessionToken, role: AdminRoleId, companyId: Option[CompanyId])
  case class AuthResult
  (
    userId: AdminUserId,
    token: SessionToken,
    newTimestamp: Option[String],
    role: AdminRoleId,
    companies: Set[CompanyId]
    )
}

class AdminSessionActor extends Actor with ActorHelper with AppActors with DefaultTimeout {

  import AdminSessionActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val adminCredentialsDataActor = lookup(AdminCredentialsDataActor.Id)
  lazy val cacheAdminSessionDataActor = lookup(CacheAdminSessionDataActor.Id)
  lazy val adminUserDataActor = lookup(AdminUserDataActor.Id)
  lazy val companyReadActor = lookup(CompanyReadActor.Id)

  lazy val timestampExpirationValue: Long = settings.Session.TimestampExpiresInSec

  def receive = {
    case Login(login, password, tokenOpt) =>
      val future = for {
        adminUserId <- getUserIdByCredentials(login, password) fallbackTo legacyPasswordVerification(login, password)
        adminUser <- getAdminUser(adminUserId)
        session <- getNewSession(adminUser, login)
      } yield LoginResult(session.adminUserId, session.id, adminUser.role, adminUser.companyId)
      future pipeTo sender

    case Authenticate(AuthCookies(token, timestampOpt)) =>
      getCacheAdminSession(token) map { cacheAdminSession =>
        val newTimestamp = newTimestampIfNecessary(timestampOpt, timestampExpirationValue)
        AuthResult(
          userId = cacheAdminSession.adminUserId,
          token = cacheAdminSession.id,
          newTimestamp = newTimestamp,
          role = cacheAdminSession.role,
          companies = cacheAdminSession.companies.toSet
        )
      } pipeTo sender

    case Logout(token) => cacheAdminSessionDataActor ! CacheAdminSessionDataActor.Delete(token)
  }

  def getUserIdByCredentials(login: String, password: String): Future[AdminUserId] =
    (adminCredentialsDataActor ? AdminCredentialsDataActor.GetCredentials(login, password)).mapTo[AdminCredentials] map (_.adminUserId)

  def legacyPasswordVerification(login: String, password: String): Future[AdminUserId] =
    (adminCredentialsDataActor ? AdminCredentialsDataActor.GetCredentialsByLogin(login)).mapTo[AdminCredentials] map { credentials =>
      credentials.legacyPassword.filter(LegacyPasswordChecker.check(password, _)) map { _ =>
        replaceLegacyPassword(login, password)
        credentials.adminUserId
      } getOrElse (throw NotFoundException("Admin user password verification has failed"))
    }

  def replaceLegacyPassword(login: String, password: String): Unit =
    adminCredentialsDataActor ! AdminCredentialsDataActor.ReplaceLegacyPassword(login, HashHelper.passwordHash(password))

  def getNewSession(user: AdminUser, login: String): Future[CacheAdminSession] =
    getAdminUserCompanyIds(user.id) flatMap { companyIds =>
      val session = CacheAdminSession(
        id = token(login),
        time = DateHelper.currentDate,
        adminUserId = user.id,
        role = user.role,
        companies = companyIds.toSeq
      )
      (cacheAdminSessionDataActor ? CacheAdminSessionDataActor.NewSession(session)).mapTo[CacheAdminSession]
    } pipeTo sender

  def getAdminUserCompanyIds(adminUserId: AdminUserId): Future[Set[CompanyId]] =
    (companyReadActor ? CompanyReadActor.GetAdminUserCompanyIds(adminUserId)).mapTo[Set[CompanyId]]

  def getCacheAdminSession(token: SessionToken): Future[CacheAdminSession] =
    (cacheAdminSessionDataActor ? CacheAdminSessionDataActor.GetSession(token)).mapTo[CacheAdminSession]

  def getAdminUser(userId: AdminUserId): Future[AdminUser] =
    (adminUserDataActor ? AdminUserDataActor.FindAdminUser(userId)).mapTo[AdminUser]

  def token(login: String) = {
    val hash = HashHelper.sha1(login + ":" + HashHelper.uuid)
    SessionToken(hash)
  }
}

case class AdminLoginResponse(user: AdminUserLogin) extends SuccessfulResponse

case class AdminUserLogin(id: AdminUserId, role: Option[AdminRoleId], @JsonProperty("companyid") companyId: Option[CompanyId])

object AdminUserLogin {

  val hiddenRole = Seq(AdminRoleId.Activator, AdminRoleId.Exporter)

  def apply(id: AdminUserId, adminRole: AdminRoleId, companyId: Option[CompanyId]): AdminUserLogin =
    AdminUserLogin(
      id = id,
      role = if(hiddenRole.contains(adminRole)) None else Some(adminRole),
      companyId = if(adminRole == AdminRoleId.Client) companyId else None
    )
}