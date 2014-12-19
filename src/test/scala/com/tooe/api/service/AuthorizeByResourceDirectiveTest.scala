package com.tooe.api.service

import akka.actor._
import spray.http.StatusCodes
import akka.testkit.TestKit
import com.tooe.core.domain.{CompanyId, SessionToken, AdminUserId, AdminRoleId}
import com.tooe.core.util.HashHelper
import scala.concurrent.Future
import spray.routing.AuthorizationFailedRejection
import com.tooe.core.usecase.AdminSessionActor.AuthResult

class AuthorizeByResourceDirectiveTest extends HttpServiceTest {

  "authorizeByResource" should {
    val f = new AuthorizeByResourceDirectiveFixture

    def getRoutee(grandAccess: Boolean) = Get(urlPrefix+f.TestServicePath) ~> f.testServiceAuthorization(grandAccess).route

    "pass requests if user is allowed to get the resource" >> {
      getRoutee(grandAccess = true) ~> check {
        status === StatusCodes.OK
      }
    }

    "reject requests if user is not allowed" >> {
      getRoutee(grandAccess = false) ~> check {
        rejection === AuthorizationFailedRejection
      }
    }
  }
}

class AuthorizeByResourceDirectiveFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  val TestServicePath = "test-service"
  val SomeRole = AdminRoleId(HashHelper.str("SomeRole"))

  abstract class TestSevice extends SprayServiceBaseHelper
    with AppAdminUserAuthHelper
    with AuthorizeByResourceHelper
  {
    type ResourceId = String

    implicit val session: AdminUserSession

    val resourceId: ResourceId = "whatever"

    val route =
      (mainPrefix & path(TestServicePath)) { _ =>
        get {
          authorizeByResource(resourceId) {
            complete(StatusCodes.OK)
          }
        }
      }

    protected def getAdminAuthResult(authCookies: AuthCookies): Future[AuthResult] = ???
  }

  def testServiceAuthorization(grandAccess: Boolean) = new TestSevice {

    implicit val session =
      AdminUserSession(
        adminUserId = AdminUserId(),
        token = SessionToken(HashHelper.uuid),
        role = SomeRole,
        companies = Set(CompanyId())
      )

    protected def checkResourceAccess(s: AdminUserSession, resourceId: ResourceId) = {
      assert(s == session)
      Future successful grandAccess
    }
  }
}