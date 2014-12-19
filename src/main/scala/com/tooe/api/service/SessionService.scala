package com.tooe.api.service

import com.tooe.core.usecase._
import akka.actor.ActorSystem
import akka.pattern.ask
import spray.http.HttpCookie
import com.tooe.core.domain.{LoginParams, SessionToken, UserId}
import com.tooe.core.usecase.SessionActor.{LoginResult, Logout}

class SessionService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 with ExecutionContextProvider {

  lazy val sessionActor = lookup(SessionActor.Id)

  import SessionService._

  val route =
    (mainPrefix & path(Path.Login) & tokenCookieOpt & entity(as[LoginParams])) { (rc: RouteContext, tokenOpt: Option[SessionToken], lp: LoginParams)  =>
      post {
        onSuccess(sessionActor.ask(SessionActor.Login(lp.userName.toLowerCase, lp.pwd, tokenOpt, rc.lang)).mapTo[LoginResult]) {
          case LoginResult(userId, token) =>
            val tokenCookie = HttpCookie(name = SessionCookies.Token, content = token.hash, path = Some("/"))
            val timestampCookie = HttpCookie(name = SessionCookies.Timestamp, content = "0", path = Some("/"))
            setCookie(tokenCookie, timestampCookie) {
              complete(LoginResponse(userId))
            }
        }
      }
    } ~
    (mainPrefix & path(Path.Logout)) { rc: RouteContext  =>
      post {
        authenticateBySession { case UserSession(_, sessionToken) =>
          sessionActor ! Logout(sessionToken)
          complete(SuccessfulResponse)
        }
      }
    }
}

object SessionService {
  object Path {
    val Login = "login"
    val Logout = "logout"
  }
}

case class LoginResponse(user: LoginResponseItem) extends SuccessfulResponse
object LoginResponse {
  def apply(userId: UserId): LoginResponse = LoginResponse(LoginResponseItem(userId))
}
case class LoginResponseItem(id: UserId)