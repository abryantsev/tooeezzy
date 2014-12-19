package com.tooe.api.service

import akka.actor._
import spray.http.StatusCodes
import akka.testkit.TestKit
import com.tooe.core.domain.{SessionToken, AdminUserId, AdminRoleId}
import spray.routing.AuthorizationFailedRejection
import com.tooe.core.util.HashHelper
import scala.concurrent.Future
import com.tooe.core.usecase.AdminSessionActor.AuthResult

class AuthorizeByRoleDirectiveTest extends HttpServiceTest {

  "authorizeByRole" should {
    val f = new AuthorizeByRoleDirectiveFixture

    def getByRole(roleId: AdminRoleId) = Get(urlPrefix+f.TestServicePath) ~> f.testServiceAuthorization(roleId).route

    import f._

    "pass requests if user has decent role 1" >> {
      getByRole(AllowedRole1) ~> check {
        status === StatusCodes.OK
      }
    }
    "pass requests if user has decent role 2" >> {
      getByRole(AllowedRole2) ~> check {
        status === StatusCodes.OK
      }
    }
    "pass requests if user has decent role 3" >> {
      getByRole(AllowedRole3) ~> check {
        status === StatusCodes.OK
      }
    }
    "reject requests if user doesn't have a decent role" >> {
      getByRole(NotAllowedRole) ~> check {
        rejection === AuthorizationFailedRejection
      }
    }
  }
}

class AuthorizeByRoleDirectiveFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  val TestServicePath = "test-service"
  val AllowedRole1, AllowedRole2, AllowedRole3, NotAllowedRole = AdminRoleId(HashHelper.str("AllowedRole"))

  abstract class TestSevice extends SprayServiceBaseHelper
    with AppAdminUserAuthHelper
    with AuthorizeByRoleHelper
  {
    implicit val session: AdminUserSession

    val route =
      (mainPrefix & path(TestServicePath)) { _ =>
        get {
          (authorizeByRole(AllowedRole2) | authorizeByRole(AllowedRole1 | AllowedRole3)) {
            complete(StatusCodes.OK)
          }
        }
      }

    protected def getAdminAuthResult(authCookies: AuthCookies): Future[AuthResult] = ???
  }

  def testServiceAuthorization(userRole: AdminRoleId) = new TestSevice {
    implicit val session =
      AdminUserSession(
        adminUserId = AdminUserId(),
        token = SessionToken(HashHelper.uuid),
        role = userRole,
        companies = Set()
      )
  }
}