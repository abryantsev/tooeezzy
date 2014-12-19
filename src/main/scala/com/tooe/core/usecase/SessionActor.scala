package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.util._
import com.tooe.core.domain.{OnlineStatusId, SessionToken, UserId}
import com.tooe.core.usecase.session.CacheSessionDataActor
import com.tooe.core.usecase.session.CacheUserOnlineDataActor
import com.tooe.core.usecase.security.CredentialsDataActor
import com.tooe.core.db.mongo.domain._
import scala.concurrent.Future
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.api.service.{ExecutionContextProvider, AuthCookies}
import com.tooe.core.db.mongo.domain.CacheSession
import java.util.Date
import com.tooe.core.db.mongo.query.UpsertResult
import com.tooe.core.db.graph.domain.FriendshipType
import com.tooe.core.db.graph.msg.{GraphFriends, GraphGetFriends}
import com.tooe.core.db.graph.GraphGetFriendsActor
import scala.collection.JavaConverters._
import com.tooe.core.exceptions.{ForbiddenAppException, NotFoundException}
import com.tooe.core.domain.SessionToken
import com.tooe.api.service.AuthCookies
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.CacheSession

object SessionActor {
  final val Id = Actors.Session
  
  case class Login(login: String, password: String, token: Option[SessionToken], lang: Lang)
  case class Logout(token: SessionToken)
  case class Authenticate(authCookies: AuthCookies)
  
  case class LoginResult(userId: UserId, token: SessionToken)
  case class AuthResult(userId: UserId, token: SessionToken, newTimestamp: Option[String])
}

class SessionActor extends AppActor with ExecutionContextProvider {
  
  lazy val credentialsDataActor = lookup(CredentialsDataActor.Id)
  lazy val cacheSessionDataActor = lookup(CacheSessionDataActor.Id)
  lazy val cacheUserOnlineDataActor = lookup(CacheUserOnlineDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val getFriendGraphActor = lookup(GraphGetFriendsActor.Id)
  lazy val infoMessageActor = lookup(InfoMessageActor.Id)

  lazy val timestampExpirationValue: Long = settings.Session.TimestampExpiresInSec

  lazy val registrationNotConfirmedMessageKey = "registration_not_confirmed_%s"
  
  import SessionActor._
  
  def receive = {
    case Login(login, password, tokenOpt, lang) =>
      val future = for {
        creds <- getCredentials(login, password).fallbackTo(legacyPasswordVerification(login, password)).flatMap {
          case creds if creds.verificationTime.isEmpty =>
            for {
               user <- getUser(creds.userId)
               template <- getMessage(registrationNotConfirmedMessageKey.format(user.gender.id), lang)
            } yield throw ForbiddenAppException(message = template.format(user.fullName))
          case creds =>
            Future.successful(creds)
        }
        session <- getNewSession(creds.userId, login)
      } yield {
        self ! Authenticate(AuthCookies(session.id, None))
        LoginResult(session.userId, session.id)
      }
      future pipeTo sender
      
    case Authenticate(AuthCookies(token, timestampOpt)) =>
      val future = for {
        userId <- getUserIdBySession(token)
        newTimestampOpt = newTimestampIfNecessary(timestampOpt)
        _ <- newTimestampOpt map { _ => updateOrInsertCacheUserOnlineStatus(userId) } getOrElse Future.successful(None)
      } yield AuthResult(userId, token, newTimestampOpt)
      future pipeTo sender
      
    case Logout(token) => cacheSessionDataActor ! CacheSessionDataActor.Delete(token)
  }

  def legacyPasswordVerification(login: String, password: String): Future[Credentials] =
    (credentialsDataActor ? CredentialsDataActor.GetCredentialsByLogin(login)).mapTo[Credentials] map { credentials =>
      credentials.legacyPasswordHash.filter(LegacyPasswordChecker.check(password, _)) map { _ =>
        replaceLegacyPassword(login, password)
        credentials
      } getOrElse (throw NotFoundException("User password verification has failed"))
    }

  def replaceLegacyPassword(login: String, password: String): Unit =
    credentialsDataActor ! CredentialsDataActor.ReplaceLegacyPassword(login, HashHelper.passwordHash(password))

  def newTimestampIfNecessary(timestampOpt: Option[String]): Option[String] =
    DateHelper.newTimestampIfNecessary(timestampOpt, timestampExpirationValue)

  def updateOrInsertCacheUserOnlineStatus(userId: UserId): Future[_] = {
    val future = getUserOnlineStatusSetting(userId) flatMap { userOnlineStatus =>
      upsertCacheUserOnlineStatus(userOnlineStatus.id, DateHelper.currentDate, userOnlineStatus.getOnlineStatus)
    }
    future onSuccess {
      case UpsertResult.Inserted => updateCacheUserOnlineFriends(userId)
    }
    future
  }

  def getMessage(id: String, lang: Lang) =
    infoMessageActor.ask(InfoMessageActor.GetMessage(id, lang)).mapTo[String]

  def updateCacheUserOnlineFriends(userId: UserId): Unit =
    getFriends(userId, FriendshipType.FRIEND) map { friends =>
      cacheUserOnlineDataActor ! CacheUserOnlineDataActor.UpdateFriends(userId, friends)
    }

  def upsertCacheUserOnlineStatus(userId: UserId, createdAt: Date, onlineStatusId: OnlineStatusId): Future[UpsertResult] =
    (cacheUserOnlineDataActor ? CacheUserOnlineDataActor.Upsert(userId, createdAt, onlineStatusId)).mapTo[UpsertResult]

  def getFriends(userId: UserId, friendGroup: FriendshipType): Future[Seq[UserId]] =
    (getFriendGraphActor ? new GraphGetFriends(userId, friendGroup)).mapTo[GraphFriends] map { graphFriends =>
      graphFriends.getFriends.asScala.toSeq
    }

  def getUserOnlineStatusSetting(userId: UserId): Future[UserOnlineStatus] = {
    userDataActor.ask(UserDataActor.GetUserOnlineStatus(userId)).mapTo[UserOnlineStatus]
  }

  def getCredentials(login: String, password: String): Future[Credentials] =
    (credentialsDataActor ? CredentialsDataActor.GetCredentials(login, password)).mapTo[Credentials]
  
  def getNewSession(userId: UserId, login: String): Future[CacheSession] =
    (cacheSessionDataActor ? CacheSessionDataActor.NewSession(userId, login)).mapTo[CacheSession]
  
  def getUserIdBySession(token: SessionToken): Future[UserId] =
    (cacheSessionDataActor ? CacheSessionDataActor.GetSession(token)).mapTo[CacheSession] map (_.userId)

  def getUser(uid: UserId) =
    userDataActor.ask(UserDataActor.GetUser(uid)).mapTo[User]

}