package com.tooe.api.service

import akka.actor.ActorSystem
import com.tooe.core.domain.{LoginParams, SessionToken}
import com.tooe.core.usecase.{AdminUserLogin, AdminLoginResponse, AdminSessionActor}
import com.tooe.core.usecase.AdminSessionActor.LoginResult
import spray.http.HttpCookie
import akka.pattern._

class AdminSessionService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  import AdminSessionService._

  private lazy val adminSessionActor = lookup(AdminSessionActor.Id)

  val route =
    (post & mainPrefix & path(Login) & admTokenCookieOpt & entity(as[LoginParams])) { (rc: RouteContext, tokenOpt: Option[SessionToken], lp: LoginParams)  =>
      onSuccess(adminSessionActor.ask(AdminSessionActor.Login(lp.userName.toLowerCase, lp.pwd, tokenOpt)).mapTo[LoginResult]) {
        case LoginResult(userId, token, role, companyId) =>
          val tokenCookie = HttpCookie(name = AdminSessionCookies.Token, content = token.hash, path = Some("/"))
          val timestampCookie = HttpCookie(name = AdminSessionCookies.Timestamp, content = "0", path = Some("/"))
          setCookie(tokenCookie, timestampCookie) {
            complete(AdminLoginResponse(AdminUserLogin(userId, role, companyId)))
          }
      }
    } ~
    (post & mainPrefix & path(Logout)) { (rc: RouteContext)  =>
      authenticateAdminBySession { s: AdminUserSession =>
        adminSessionActor ! AdminSessionActor.Logout(s.token)
        complete(SuccessfulResponse)
      }
    }
}

object AdminSessionService {
  val Login = "admlogin"
  val Logout = "admlogout"
}
