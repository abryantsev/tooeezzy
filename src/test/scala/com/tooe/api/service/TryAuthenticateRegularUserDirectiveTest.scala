package com.tooe.api.service

import akka.actor._
import spray.http.StatusCodes
import akka.testkit.TestKit
import com.tooe.core.usecase.SessionActor
import com.tooe.core.domain.UserId
import scala.concurrent.Future
import spray.routing.{AuthenticationFailedRejection, MissingCookieRejection}
import com.tooe.core.exceptions.ApplicationException

class TryAuthenticateRegularUserDirectiveTest extends HttpServiceTest with SessionCookieFixture {

  "tryAuthenticateRegularUser" should {
    val f = new TryAuthenticateBySessionDirectiveFixture(userId)
    "reject not authenticated request" >> {
      Get(urlPrefix+f.TestServicePath) ~> f.testServiceAuthPasses().route ~> check {
        rejection === MissingCookieRejection(SessionCookies.Token)
      }
    }
    "pass authenticated requests" >> {
      Get(urlPrefix+f.TestServicePath).withHeaders(SessionTokenCookie) ~> f.testServiceAuthPasses().route ~> check {
        status === StatusCodes.OK
      }
    }
    "do not pass not authenticated requests" >> {
      Get(urlPrefix+f.TestServicePath).withHeaders(SessionTokenCookie) ~> f.testServiceAuthFails.route ~> check {
        rejection === AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, Nil)
      }
    }
    "pass authenticated request and set Timestamp cookie if necessary" >> {
      Get(urlPrefix+f.TestServicePath).withHeaders(SessionTokenCookie) ~>
        f.testServiceAuthPasses(newTimestamp = Some("some-new-timestamp")).route ~> check 
      {
        status === StatusCodes.OK
        header("Set-Cookie").get.value === s"${SessionCookies.Timestamp}=some-new-timestamp; Path=/"
      }
    }
    "pass authenticated request and set Timestamp cookie if not necessary" >> {
      Get(urlPrefix+f.TestServicePath).withHeaders(SessionTokenCookie) ~>
        f.testServiceAuthPasses(newTimestamp = None).route ~> check 
      {
        status === StatusCodes.OK
        header("Set-Cookie") === None
      }
    }
    "passes timestamp cookie" >> {
      todo
    }
  }
}

class TryAuthenticateBySessionDirectiveFixture(userId: UserId)(implicit actorSystem: ActorSystem) extends TestKit(actorSystem)
{
  val TestServicePath = "test-service"

  abstract class TestSevice
    extends SprayServiceBaseHelper
    with AppRegularUserAuthHelper
  {
    val route =
      (mainPrefix & path(TestServicePath)) { implicit routeContext: RouteContext =>
        get {
          tryAuthenticateRegularUser { session =>
            complete(StatusCodes.OK)
          }
        }
      }
  }
  
  def testServiceAuthPasses(newTimestamp: Option[String] = None) = new TestSevice {
    import SessionActor._
    override def getAuthResult(authCookies: AuthCookies) =
      Future successful AuthResult(userId, authCookies.token, newTimestamp)
  }
  
  lazy val testServiceAuthFails = new TestSevice {
    override def getAuthResult(authCookies: AuthCookies) =
      Future failed ApplicationException(statusCode = StatusCodes.Unauthorized)
  }
}