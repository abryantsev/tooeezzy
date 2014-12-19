package com.tooe.core.usecase

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestActorRef
import akka.testkit.TestProbe
import com.tooe.core.domain.{OnlineStatusId, SessionToken, UserId}
import com.tooe.core.usecase.session.{CacheUserOnlineDataActor, CacheSessionDataActor}
import com.tooe.api.service.AuthCookies
import scala.concurrent.Future
import com.tooe.core.db.mongo.domain.{User, UserOnlineStatus, UserFixture}
import java.util.Date
import com.tooe.core.db.mongo.query.UpsertResult
import com.tooe.core.db.graph.domain.FriendshipType
import com.tooe.core.service.CredentialsFixture
import com.tooe.core.util.Lang

class SessionActorTest extends ActorTestSpecification {

  "SessionActor" should {
    
    "create new session on login" >> {
      val f = new SessionActorFixtureBase {}
      f.sessionActor ! SessionActor.Login("login-name", "", None, null)
      f.cacheSessionDataProbe expectMsg CacheSessionDataActor.NewSession(f.user.id, "login-name")
      success
    }
    
    "check session during authentication" >> {
      val f = new SessionActorFixtureBase {}
      f.sessionActor ! SessionActor.Authenticate(f.authCookies)
      f.cacheSessionDataProbe expectMsg CacheSessionDataActor.GetSession(f.token)
      success
    }
    
    "delete session on logout" >> {
      val f = new SessionActorFixtureBase {}
      f.sessionActor ! SessionActor.Logout(f.token)
      f.cacheSessionDataProbe expectMsg CacheSessionDataActor.Delete(f.token)
      success
    }

    "on Authenticate updateOrInsertCacheUserOnlineStatus" >> {
      def fixture(upsertCacheUserOnlineStatusResult: UpsertResult) = new SessionActorFixture {
        val probe = TestProbe()
        override def sessionActorFactory = new SessionActor {
          override def newTimestampIfNecessary(timestampOpt: Option[String]) = Some("new timestamp")
          override def getUserIdBySession(token: SessionToken) = Future successful user.id
          override def getUserOnlineStatusSetting(userId: UserId) = Future successful user
          override def upsertCacheUserOnlineStatus(userId: UserId, createdAt: Date, onlineStatusId: OnlineStatusId) =
            Future successful upsertCacheUserOnlineStatusResult
          override def updateCacheUserOnlineFriends(userId: UserId) = probe.ref ! ("updateCacheUserOnlineFriends", userId)
        }
      }
      "updateCacheUserOnlineFriends when new session created" >> {
        val f = fixture(upsertCacheUserOnlineStatusResult = UpsertResult.Inserted)
        f.sessionActor ! SessionActor.Authenticate(f.authCookies)
        f.probe expectMsg (("updateCacheUserOnlineFriends", f.user.id))
        success
      }
      "don't updateCacheUserOnlineFriends when session exists" >> {
        val f = fixture(upsertCacheUserOnlineStatusResult = UpsertResult.Updated)
        f.sessionActor ! SessionActor.Authenticate(f.authCookies)
        f.probe expectNoMsg ()
        success
      }
    }

    "updateCacheUserOnlineFriends" >> {
      val f = new SessionActorFixture {
        val probe = TestProbe()
        val userId, friend1, friend2 = UserId()
        override def sessionActorFactory = new SessionActor {
          override lazy val cacheUserOnlineDataActor = probe.ref
          override def getFriends(userId: UserId, friendGroup: FriendshipType) = Future successful Seq(friend1, friend2)
        }
      }
      import f._
      sessionActor.underlyingActor.updateCacheUserOnlineFriends(f.userId)
      probe expectMsg CacheUserOnlineDataActor.UpdateFriends(userId, Seq(friend1, friend2))
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class SessionActorFixtureBase(implicit system: ActorSystem) extends SessionActorFixture {
  lazy val cacheSessionDataProbe = TestProbe()

  def sessionActorFactory = new SessionActor {
    override lazy val credentialsDataActor = null
    override lazy val cacheSessionDataActor = cacheSessionDataProbe.ref
    override def getCredentials(login: String, password: String) = Future.successful(creds)
    override def getUserOnlineStatusSetting(userId: UserId): Future[UserOnlineStatus] = Future.successful(user)
    override def getUser(uid: UserId): Future[User] = Future.successful(user)
    override def getMessage(id: String, lang: Lang) = super.getMessage(id, lang)
  }
}

abstract class SessionActorFixture(implicit system: ActorSystem) {
  val token = SessionToken("some-token")
  val otherToken = SessionToken("other-token")
  val authCookies = AuthCookies(token, None)
  val user = new UserFixture().user
  val creds = new CredentialsFixture().credentials.copy(uid = user.id.id)

  def sessionActorFactory: SessionActor

  lazy val sessionActor = TestActorRef[SessionActor](Props(sessionActorFactory))
}