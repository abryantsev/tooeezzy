package com.tooe.core.usecase

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestKit}
import com.tooe.core.db.mongo.domain.Location
import com.tooe.core.db.mongo.domain.PreModerationCompany
import com.tooe.core.db.mongo.domain.PreModerationLocation
import com.tooe.core.db.mongo.domain.Product
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain.AdminUserId
import com.tooe.core.domain.CompanyId
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.PreModerationCompanyId
import com.tooe.core.domain.PreModerationLocationId
import com.tooe.core.domain.PresentCode
import com.tooe.core.domain.ProductId
import com.tooe.core.domain._
import com.tooe.core.service._
import scala.concurrent.Future

class AuthorizationActorTest extends ActorTestSpecification {

  "AuthorizationActor" should {
    "return failure for unknown resource type" >> {
      val fixture = new AuthorizationActorFixture {
        val incorrectResourceType = new ObjectiveId {}
        def authorizationActorFactory = new AuthorizationActorUnderTest
      }
      import fixture._
      val probe = authorizationActor probeTells checkResourceAccessMsg(incorrectResourceType)
      probe.expectMsgType[akka.actor.Status.Failure]
      success
    }
    "Company" >> {
      val fixture = new AuthorizationActorFixture {
        def authorizationActorFactory = new AuthorizationActorUnderTest
      }
      import fixture._
      authorizationActor probeTells checkResourceAccessMsg(companyId) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(companyId, companyId = CompanyId()) expectMsg false
      success
    }
    "Location" >> {
      val fixture = new AuthorizationActorFixture {
        override val adminRoleId = AdminRoleId.Client
        val location = new LocationFixture().entity.copy(companyId = companyId)
        def authorizationActorFactory = new AuthorizationActorUnderTest {
          override def getLocation(id: LocationId) = Future successful location
          override def getLocationWithAnyLifeCycleStatus(id: LocationId): Future[Location] = Future successful location
        }
      }
      import fixture._
      authorizationActor probeTells checkResourceAccessMsg(location.id) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(location.id, companyId = CompanyId()) expectMsg false
      success
    }
    "Product" >> {
      val fixture = new AuthorizationActorFixture {
        val product = new ProductFixture().product.copy(companyId = companyId)
        def authorizationActorFactory = new AuthorizationActorUnderTest {
          override def getProduct(id: ProductId) = Future successful product
        }
      }
      import fixture._
      authorizationActor probeTells checkResourceAccessMsg(product.id) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(product.id, companyId = CompanyId()) expectMsg false
      success
    }
    "PreModerationCompany" >> {
      val fixture = new AuthorizationActorFixture {
        val companyMod = new PreModerationCompanyFixture().company.copy(agentId = adminUserId)
        def authorizationActorFactory = new AuthorizationActorUnderTest {
          override def getCompanyMod(id: PreModerationCompanyId) = Future successful companyMod
        }
      }
      import fixture._
      authorizationActor probeTells checkResourceAccessMsg(companyMod.id) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(companyMod.id, adminUserId = AdminUserId()) expectMsg false
      success
    }
    "PreModerationLocation" >> {
      val fixture = new AuthorizationActorFixture {
        override val adminRoleId = AdminRoleId.Client
        val locationMod = new PreModerationLocationFixture().entity.copy(companyId = companyId)
        def authorizationActorFactory = new AuthorizationActorUnderTest {
          override def getLocationMod(id: PreModerationLocationId) = Future successful locationMod
        }
      }
      import fixture._
      authorizationActor probeTells checkResourceAccessMsg(locationMod.id) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(locationMod.id, companyId = CompanyId()) expectMsg false
      success
    }
    "Present" >> {
      val fixture = new AuthorizationActorFixture {
        override val adminRoleId = AdminRoleId.Activator
        val present = PresentFixture.present(companyId = companyId)
        def authorizationActorFactory = new AuthorizationActorUnderTest {
          override def getPresentByCode(code: PresentCode) = Future.successful(present)
          override def getPresent(id: PresentId) = Future.successful(present)
        }
      }
      import fixture._
      authorizationActor probeTells checkResourceAccessMsg(present.id) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(present.id, companyId = CompanyId()) expectMsg false
      authorizationActor probeTells checkResourceAccessMsg(present.code) expectMsg true
      authorizationActor probeTells checkResourceAccessMsg(present.code, companyId = CompanyId()) expectMsg false
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class AuthorizationActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  val adminUserId = AdminUserId()
  val companyId = CompanyId()
  val adminRoleId = AdminRoleId.Admin

  def checkResourceAccessMsg(resourceId: ObjectiveId, adminUserId: AdminUserId = adminUserId, companyId: CompanyId = companyId) =
    AuthorizationActor.CheckResourceAccess(adminUserId, adminRoleId, Set(companyId), resourceId)

  class AuthorizationActorUnderTest extends AuthorizationActor {
    override def getLocation(id: LocationId): Future[Location] = ???
    override def getProduct(id: ProductId): Future[Product] = ???
    override def getCompanyMod(id: PreModerationCompanyId): Future[PreModerationCompany] = ???
    override def getLocationMod(id: PreModerationLocationId): Future[PreModerationLocation] = ???
    override def getPresentByCode(code: PresentCode): Future[Present] = ???
    override def getPresent(id: PresentId): Future[Present] = ???
  }

  def authorizationActorFactory: AuthorizationActorUnderTest

  lazy val authorizationActor = TestActorRef[AuthorizationActorUnderTest](Props(authorizationActorFactory))
}