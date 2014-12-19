package com.tooe.api.service

import akka.actor._
import spray.http.StatusCodes
import akka.testkit.TestKit
import com.tooe.core.usecase.SessionActor
import akka.testkit.TestActorRef
import akka.actor.Props
import akka.testkit.TestProbe
import com.tooe.core.domain.LoginParams

class SessionServiceTest extends HttpServiceTest with SessionCookieFixture {

  import SessionService._
  
  "SessionService" should {
    "let login and set " >> {
      val f = new SessionServiceFixture.Login(sessionCookieFixture = this)
      Post(urlPrefix+Path.Login, f.Request) ~> f.sessionService.route ~> check {
        status === StatusCodes.OK
        headers.map(_.value) === 
          s"${SessionCookies.Token}=some-session-token; Path=/" :: 
          s"${SessionCookies.Timestamp}=0; Path=/" :: // invalidate previous timestamp cookie to update online status next authentication step 
          Nil
      }
    }
    "let logout and delete session" >> {
      val f = new SessionServiceFixture.WithProbe
      Post(urlPrefix+Path.Logout).withHeaders(SessionTokenCookie) ~> f.sessionService.route ~> check {
        f.probe expectMsg SessionActor.Logout(sessionToken)
        entity.asString === """{"meta":{"status":"success"}}"""
        status === StatusCodes.OK
      }
    }
  }
}

object SessionServiceFixture {

  class Login(sessionCookieFixture: SessionCookieFixture)(implicit actorSystem: ActorSystem) extends SessionServiceFixture {

    def sessionActorRef = TestActorRef[SessionActorMock](Props(new SessionActorMock))(actorSystem)

    val Request = LoginParams("some-login", "some-password")

    class SessionActorMock extends Actor {
      import SessionActor._
      def receive = {
        case Login(login, password, token, lang) =>
          assert(login == Request.userName)
          assert(password == Request.pwd)
          sender ! LoginResult(sessionCookieFixture.userId, sessionCookieFixture.sessionToken)
      }
    }
  }

  class WithProbe(implicit actorSystem: ActorSystem) extends SessionServiceFixture {
    val probe = TestProbe()
    def sessionActorRef = probe.ref
  }
}

abstract class SessionServiceFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  
  def sessionActorRef: ActorRef
  
  lazy val sessionService = new SessionService with ServiceAuthMock {
    override lazy val sessionActor = sessionActorRef
  }
}