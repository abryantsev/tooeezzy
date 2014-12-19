package com.tooe.core.usecase

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import com.tooe.api.service.RouteContext
import com.tooe.core.db.mongo.domain._
import com.tooe.core.domain._
import com.tooe.core.service.{PreModerationLocationFixture, AdminUserFixture, LocationFixture, ProductFixture}
import com.tooe.core.usecase.location.{PreModerationLocationDataActor, LocationDataActor}
import com.tooe.core.util.Lang
import scala.Some
import scala.concurrent.Future

class ProductWriteActorTest extends ActorTestSpecification {

  "ProductWriteActor component methods" should {
    "increment product counter and activate locations" >> {
      val f = new ProductActorWriteFixture {}
      import f._
      productWriteActor ! ProductWriteActor.SaveProduct(request, null, RouteContext("v01", Lang.ru.id))
      updateStatisticProbe.expectMsgAllOf(
        UpdateStatisticActor.ChangeLocationProductsCounter(location.id, 1)
      )
      locationDataProbe.expectMsgAllOf(
        LocationDataActor.UpdateLifecycleStatus(location.id, None)
      )
      locationModDataProbe.expectMsgAllOf(
        PreModerationLocationDataActor.UpdateLifecycleStatus(locationMod.id, None)
      )
      success
    }

    "deactivate locations" >> {
      val f = new ProductActorWriteFixture {}
      import f._
      productWriteActor ! ProductWriteActor.DeleteProduct(product.id)
      locationDataProbe.expectMsgAllOf(
        LocationDataActor.UpdateLifecycleStatus(location.id, Some(LifecycleStatusId.Deactivated))
      )
      locationModDataProbe.expectMsgAllOf(
        PreModerationLocationDataActor.UpdateLifecycleStatus(locationMod.id, Some(LifecycleStatusId.Deactivated))
      )
      updateStatisticProbe.expectMsgAllOf(
        UpdateStatisticActor.ChangeLocationProductsCounter(location.id, -1)
      )
      success
    }

  }

  step {
    system.shutdown()
  }
}

abstract class ProductActorWriteFixture(implicit system: ActorSystem) {
  val location = new LocationFixture().entity
  val product = new ProductFixture().product.copy(location = LocationWithName(location.id, Map("ru" -> "name")))
  val locationMod = new PreModerationLocationFixture().entity
  val admin = new AdminUserFixture().adminUser
  val request = SaveProductRequest(null, null, null, null, null, null, null, null, null, location.id, null, null, null)

  val updateStatisticProbe, locationDataProbe, locationModDataProbe, urlWriteProbe = TestProbe()

  class ProductWriteActorUnderTest extends ProductWriteActor {

    override lazy val updateStatisticActor: ActorRef = updateStatisticProbe.ref

    override lazy val locationDataActor = locationDataProbe.ref

    override lazy val locationModDataActor = locationModDataProbe.ref

    override lazy val urlsWriteActor = urlWriteProbe.ref

    override def getLocation(request: SaveProductRequest): Future[Location] = Future.successful(location)

    override def getLocationMod(id: LocationId) = Future.successful(locationMod)

    override def getActiveLocationOrByLifeCycleStatuses(request: SaveProductRequest, statuses: Seq[LifecycleStatusId]): Future[Location] = Future.successful(location)

    override def getProduct(productId: ProductId) = Future.successful(product)

    override def saveProduct(ctx: RouteContext, request: SaveProductRequest, location: Location, companyId: CompanyId): Future[Product] = Future.successful(product)

    override def getAdminUser(adminUserId: AdminUserId) = Future.successful(admin)

    override def countProductsForLocation(id: LocationId) = Future.successful(1)

  }

  def productActorFactory: ProductWriteActor = new ProductWriteActorUnderTest()

  lazy val productWriteActor = TestActorRef[ProductWriteActor](Props(productActorFactory))
}
