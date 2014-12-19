package com.tooe.api.service

import spray.http._
import spray.routing._
import spray.routing.authentication.ContextAuthenticator
import scala.concurrent.Future
import com.tooe.core.usecase.AdminSessionActor
import com.tooe.core.domain._
import com.tooe.core.usecase.SessionActor.AuthResult
import com.tooe.core.exceptions.ApplicationException
import spray.routing.AuthenticationFailedRejection

object SessionCookies {
  val Token = "tooe_token"
  val Timestamp = "tmp_time"
}

object AdminSessionCookies {
  val Token = "tooe_adm_token"
  val Timestamp = "adm_tmp_time"
}

case class AuthCookies(token: SessionToken, timestampOpt: Option[String])

trait AppUserAuthBaseHelper {
  import Directives._

  def sessionToken(cookie: HttpCookie) = SessionToken(cookie.content)

  sealed trait AnySession

  protected def updateTimestamp(cookieName: String, newTimestamp: Option[String]) = {
    newTimestamp map { timestamp =>
      val timeCookie = HttpCookie(name = cookieName, content = timestamp, path = Some("/"))
      setCookie(timeCookie)
    } getOrElse noop
  }
}

trait AppRegularUserAuthHelper extends AppUserAuthBaseHelper {
  import scala.concurrent.ExecutionContext.Implicits.global
  import Directives._

  case class UserSession(userId: UserId, token: SessionToken) extends AnySession

  val tokenCookieOpt = optionalCookie(SessionCookies.Token) map (_ map sessionToken)
  private val tokenCookie = cookie(SessionCookies.Token) map sessionToken
  private val timestampCookieOpt = optionalCookie(SessionCookies.Timestamp) map (_ map (_.content))
  private val authCookies = (tokenCookie & timestampCookieOpt) as AuthCookies

  protected def getAuthResult(authCookies: AuthCookies): Future[AuthResult]

  private def sessionAuthenticator(authCookies: AuthCookies): ContextAuthenticator[AuthResult] = ctx =>
    getAuthResult(authCookies) map (Right(_))

  private def sessionAuthenticatorWithRejection(authCookies: AuthCookies): ContextAuthenticator[AuthResult] = ctx =>
    getAuthResult(authCookies) map (Right(_)) recover {
      case e: ApplicationException => Left(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, Nil))
    }

  /**
   * rejects if authentication fails that allows to do another authentication after, whereas authenticateRegularUser returns 401 immediately
   */
  val tryAuthenticateRegularUser: Directive1[UserSession] = authenticateRegularUser(sessionAuthenticatorWithRejection)

  val authenticateRegularUser: Directive1[UserSession] = authenticateRegularUser(sessionAuthenticator)

  val authenticateBySession = authenticateRegularUser //old naming

  private def authenticateRegularUser(authenticator: AuthCookies => ContextAuthenticator[AuthResult]): Directive1[UserSession] =
    authCookies flatMap { authCookies =>
      authenticate(authenticator(authCookies))
    } flatMap { authData =>
      updateTimestamp(SessionCookies.Timestamp, authData.newTimestamp) & provide(UserSession(authData.userId, authData.token))
    }
}

trait AppAdminUserAuthHelper extends AppUserAuthBaseHelper {
  import scala.concurrent.ExecutionContext.Implicits.global
  import Directives._

  case class AdminUserSession
  (
    adminUserId: AdminUserId,
    token: SessionToken,
    role: AdminRoleId,
    companies: Set[CompanyId]
    ) extends AnySession

  val admTokenCookieOpt = optionalCookie(AdminSessionCookies.Token) map (_ map sessionToken)
  private val admTokenCookie = cookie(AdminSessionCookies.Token) map sessionToken
  private val admTimestampCookieOpt = optionalCookie(AdminSessionCookies.Timestamp) map (_ map (_.content))
  private val admAuthCookies = (admTokenCookie & admTimestampCookieOpt) as AuthCookies

  protected def getAdminAuthResult(authCookies: AuthCookies): Future[AdminSessionActor.AuthResult]

  private def sessionAdminAuthenticator(authCookies: AuthCookies): ContextAuthenticator[AdminSessionActor.AuthResult] =
    ctx => getAdminAuthResult(authCookies) map (Right(_))

  val authenticateAdminUser: Directive1[AdminUserSession] = admAuthCookies flatMap { authCookies =>
    authenticate(sessionAdminAuthenticator(authCookies))
  } flatMap { authData =>
    def session = AdminUserSession(
      adminUserId = authData.userId,
      token = authData.token,
      role = authData.role,
      companies = authData.companies
    )
    updateTimestamp(AdminSessionCookies.Timestamp, authData.newTimestamp) & provide(session)
  }

  val authenticateAdminBySession = authenticateAdminUser //old naming
}

trait AuthorizeByRoleHelper { self: AppAdminUserAuthHelper =>
  import Directives._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit def roleToSet(roleId: AdminRoleId): Set[AdminRoleId] = Set(roleId)

  implicit def enrichRoleSet(set: Set[AdminRoleId]) = new {
    def |(roleId: AdminRoleId) = set + roleId
  }

  trait AuthorizeByRoleMagnet {
    def apply(): Directive0
  }

  object AuthorizeByRoleMagnet {

    implicit def fromOneRoleId(roleId: AdminRoleId)(implicit session: AdminUserSession) =
      new AuthorizeByRoleMagnet {
        def apply() = authorize(roleId == session.role)
      }

    implicit def fromManyRoleIds(roleIds: Set[AdminRoleId])(implicit session: AdminUserSession) =
      new AuthorizeByRoleMagnet {
        def apply() = authorize(roleIds contains session.role)
      }
  }

  def authorizeByRole(magnet: AuthorizeByRoleMagnet): Directive0 = magnet()
}

trait AuthorizeByResourceHelper { self: AppAdminUserAuthHelper =>
  import Directives._
  import scala.concurrent.ExecutionContext.Implicits.global

  type ResourceId

  trait AuthorizeByResourceMagnet {
    def apply(): Directive0
  }

  object AuthorizeByResourceMagnet {

    implicit def fromLocationId(resourceId: ResourceId)(implicit session: AdminUserSession) =
      new AuthorizeByResourceMagnet {
        def apply() = authorizeByResource(checkResourceAccess(session, resourceId))
      }

    private def authorizeByResource(checkFun: => Future[Boolean]): Directive0 =
      onSuccess(checkFun) flatMap { result => authorize(result) }
  }

  def authorizeByResource(magnet: AuthorizeByResourceMagnet): Directive0 = magnet()

  protected def checkResourceAccess(session: AdminUserSession, resourceId: ResourceId): Future[Boolean]
}

trait AppAdminUserAuthorizeHelper extends AuthorizeByRoleHelper with AuthorizeByResourceHelper {
  self: AppAdminUserAuthHelper =>

  import AdminRoleId._

  type ResourceId = ObjectiveId

  val SuperUser = Admin | Moderator
  val AnyAgent = Agent | Superagent | Dealer | Superdealer

  def authorizeAccess(magnet: AuthorizeAccessMagnet): Directive0 = magnet()

  trait AuthorizeAccessMagnet {
    def apply(): Directive0
  }

  object AuthorizeAccessMagnet {

    implicit def fromResourceId(resourceId: ResourceId)(implicit session: AdminUserSession) =
      new AuthorizeAccessMagnet {
        def apply() = authorizeByRole(SuperUser) | authorizeByRole(AnyAgent) & authorizeByResource(resourceId)
      }
  }
}

trait AppAuthHelper
  extends AppRegularUserAuthHelper
  with AppAdminUserAuthHelper
  with AppAdminUserAuthorizeHelper
{
  val authenticateAnyUser: Directive1[AnySession] = authenticateRegularUserGen | authenticateAdminUserGen

  private def authenticateRegularUserGen = tryAuthenticateRegularUser map (_.asInstanceOf[AnySession])
  private def authenticateAdminUserGen = authenticateAdminBySession map (_.asInstanceOf[AnySession])

//  trait Permission
//  case class EntityPermission(entity: String, actions: String*) extends Permission
//  case class ItemPermission(entity: String, itemId: org.bson.types.ObjectId, actions: String*) extends Permission
//
//  def getAuthorizationResult(userId: UserId, permissions: Permission*): Boolean
//
//  def authoriseUser(userSession: UserSession, permissions: Permission*) = {
//    authorize(getAuthorizationResult(userSession.userId, permissions: _*))
//  }
}