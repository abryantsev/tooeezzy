package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem, ActorRef}
import com.tooe.core.db.mongo.domain.UserFixture
import com.tooe.core.service._
import scala.concurrent.{Promise, Future}
import com.tooe.api.service.OffsetLimit
import com.tooe.core.util.Lang
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.usecase.present.PresentDataActor
import java.math.BigInteger
import com.tooe.core.domain._
import com.tooe.core.domain.PresentId
import com.tooe.core.domain.LocationId
import com.tooe.api.service.GetPresentParameters
import com.tooe.core.service.PresentAdminSearchParams
import com.tooe.core.db.mongo.domain.Present
import com.tooe.core.domain.UserId
import com.tooe.api.service.RouteContext
import scala.util.Success
import org.bson.types.ObjectId
import com.tooe.core.usecase.user.response.{ActorShortItem, ActorItem}

class PresentReadActorTest extends ActorTestSpecification {

  "PresentReadActor" should {

    "return present items" >> {
      val f = new PresentActorReadGetPresentsFixture {
        def presentReadActorFactory = new PresentReadActor {
          override def getPresents(ids: Set[PresentId]) = {
            assert(ids === presentIds)
            Future successful Seq(present)
          }
        }
      }
      import f._
      presentReadActor ! PresentReadActor.GetUserEventPresents(presentIds)
      val msg = expectMsgType[Seq[UserEventPresentItem]]
      val item = msg.head
      item.id === present.id
      item.name === present.product.productName
      item.media.imageUrl must containing(present.product.media.get.url.url.id)
    }
    "getPresents" >> {
      val f = new PresentActorReadGetPresentsFixture {
        def presentReadActorFactory = new PresentReadActor {
          override lazy val presentDataActor = probe.ref
        }
      }
      import f._
      underlyingReadActor.getPresents(presentIds)
      probe expectMsg PresentDataActor.Find(presentIds)
      success
    }
    "send UpdateUserPresentsCounters on GetPresents request" >> {
      val f = new PresentReadActorFixture {
        val probe = TestProbe()

        override def presentActorFactory = new PresentReadActorUnderTest {
          override lazy val updateStatisticActor: ActorRef = probe.ref
        }
      }
      import f._

      presentActor ! PresentReadActor.GetPresents(user.id, null, null, RouteContext("v01", "ru"))
      probe expectMsg UpdateStatisticActor.SetUserNewPresentsCounter(user.id, 0)
      success
    }

    "find presents" >> {
      val f = new PresentReadActorFixture {
      }
      import f._
      import akka.pattern.ask
      import scala.concurrent.ExecutionContext.Implicits.global

      presentActor.ask(PresentReadActor.GetPresentsAdminSearch(adminSearchRequest)).mapTo[GetPresentsAdminSearchResponse].foreach(response => {
        val expected = presentsPromise.awaitResult
        response.presentscount === 3
        response.presentscount === expected.size
        response.presents.size === expected.size
        response.presents.zip(expected).foreach {
          case (ri, p) =>
            ri.id === p.id.id
            ri.product.id === p.product.productId.id.toString
            ri.product.name === p.product.productName
            ri.product.`type` === p.product.productTypeId.id
            ri.product.media.imageUrl must containing(p.product.media.get.url.url.id)
            ri.creationtime === p.createdAt
        }

      })
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class PresentReadActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  import scala.concurrent.ExecutionContext.Implicits.global

  val user = new UserFixture().user
  val present = PresentFixture.present(userId = Some(user.id))
  val presentsCount = 1L
  val location = new LocationFixture().entity
  val photoUrlMap = Map("" -> "")
  val adminSearchRequest = PresentAdminSearchRequest(Some("Cool name"), LocationId(), None, None, OffsetLimit())
  val presentsPromise = Promise[Seq[Present]]()
  val presentsIds = (1 to 3).map(_ => new PresentId())
  val productIds = (1 to 3).map(_ => new ProductId(new ObjectId()))

  class PresentReadActorUnderTest extends PresentReadActor {
    override def getPresentsAndCountFtr(userId: UserId, parameters: GetPresentParameters, offsetLimit: OffsetLimit): Future[(Seq[Present], Long)] =
      Future successful ((Seq(present), presentsCount))

    override def findActorsShort(userIds: Set[UserId], lang: Lang, imageSize: String): Future[Seq[ActorShortItem]] =
      Future successful Seq(ActorShortItem(_ => None, imageSize)(user))

    override def findLocations(presents: Seq[Present], lang: Lang): Future[Seq[UserEventLocation]] =
      Future successful Seq(UserEventLocation(lang, "100x100")(location))

    override def findPresents(params: PresentAdminSearchParams) =
      Future.successful(presentsIds.zip(productIds).map{
        case (prid, proid) =>
          PresentFixture.present(id = prid, productId = proid, productName = params.productName.getOrElse(""), locationId = params.locationId, presentStatusId = params.status.getOrElse(PresentStatusId.valid))
      })
        .andThen {
        case Success(presents) => presentsPromise.success(presents.toSeq)
      }

    override def countPresents(params: PresentAdminSearchParams) = Future.successful(presentsIds.size)


    override def getProductSnapshots(ids: Seq[BigInteger]) = Future(ids.zip(productIds).map{
      case (oid, pid) => {
        val productSnapshot = new PaymentFixture().productSnapshot.copy(productId = pid.id.toString)
        productSnapshot.orderJpaId = oid
        productSnapshot
      }
    })

    override def getPayments(ids: Seq[Payment.OrderId]) = Future(ids.map(id => {
      val payment = new PaymentFixture().payment
      payment.orderJpaId = id.bigInteger
      payment
    }))

  }

  def presentActorFactory = new PresentReadActorUnderTest

  lazy val presentActor = TestActorRef[PresentReadActorUnderTest](Props(presentActorFactory))
}

abstract class PresentActorReadGetPresentsFixture(implicit system: ActorSystem) {
  val probe = TestProbe()

  val OrderId: Payment.OrderId = BigInt(100)
  val payment = new PaymentFixture().payment
  val recipient = payment.recipient
  val productSnapshot = payment.productSnapshot

  val present = PresentFixture.present()
  val presentIds = Set(present.id)

  def presentReadActorFactory: PresentReadActor

  lazy val presentReadActor = TestActorRef[PresentReadActor](Props(presentReadActorFactory))

  def underlyingReadActor = presentReadActor.underlyingActor
}