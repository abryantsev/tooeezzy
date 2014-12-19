package com.tooe.core.usecase

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe}
import com.tooe.core.db.mongo.domain.{Wish, Present}
import com.tooe.core.db.mysql.domain.Payment
import com.tooe.core.domain.{UserId, ProductTypeId}
import com.tooe.core.service.PaymentFixture
import com.tooe.core.service.PresentFixture
import com.tooe.core.usecase.payment.PaymentDataActor
import com.tooe.core.usecase.present.PresentDataActor
import com.tooe.core.usecase.product.ProductDataActor
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.wish.WishDataActor
import scala.concurrent.Future
import scala.concurrent.Promise

class PresentWriteActorTest extends ActorTestSpecification {

  "PresentWriteActor component methods" should {
    "make present" >> {
      val f = new PresentActorWriteFixture {
        val promise = Promise[Present]()
        def presentWriteActorFactory = new PresentWriteActor {
          override def getPayment(orderId: Payment.OrderId) = Future successful payment
          override def savePresent(present: Present) = {
            promise success present
            present
          }
        }
      }
      f.presentWriteActor ! PresentWriteActor.MakePresent(f.OrderId)
      val r = f.promise.awaitResult
      r.id !== null
      r.code.value === f.OrderId.toString
      r.userId === f.recipient.getRecipientId
      r.senderId === f.payment.getUserId
      r.hideSender === f.payment.recipient.hideSender
      r.message === f.payment.productSnapshot.getMessage
      r.createdAt !== null
      r.expiresAt !== null
      r.expiresAt must beGreaterThan (r.createdAt)
      (r.expiresAt.getTime - r.createdAt.getTime) === (f.payment.productSnapshot.validity * 24*60*60*1000)
      r.product !== null
      r.product.locationId === f.productSnapshot.getLocationId
      r.product.productId === f.productSnapshot.getProductId
      r.product.productName === f.productSnapshot.name
      r.product.media !== null
      r.product.media.get.url.url.id === f.payment.productSnapshot.picture_url
      r.product.media.get.description === None //TODO
      r.orderId === f.OrderId
      r.adminComment === None
    }
    "call fulfillWish, addGift, createUserEvent and createNews onSuccess" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override def getPayment(orderId: Payment.OrderId) = Future successful payment
          override def addGift(present: Present) = probe.ref ! "addGift"
          override def createUserEvent(present: Present) = probe.ref ! "createUserEvent"
          override def fulfillWish(present: Present) = probe.ref ! "fulfillWish"
          override def incrementSalesCount(present: Present) = probe.ref ! "incrementSalesCount"
          override def incrementPresentsCount(present: Present) = probe.ref ! "incrementPresentsCount"
          override def createNews(present: Present) = probe.ref ! "createNews"
          override def sendNotifications(present: Present) = probe.ref ! "sendNotifications"
          override def addUrlsForPresentMedia(present: Present) = probe.ref ! "addUrlsForPresentMedia"
        }
      }
      import f._
      presentWriteActor ! PresentWriteActor.MakePresent(OrderId)
      probe expectMsgAllOf (
        "addGift", "createUserEvent", "fulfillWish", "incrementSalesCount", "incrementPresentsCount", "createNews",
        "addUrlsForPresentMedia", "sendNotifications"
      )
      success
    }
    "fulfills wish" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val wishDataActor = probe.ref
        }
      }
      import f._
      underlyingWriteActor.tryToFulfillWish(present)
      probe expectMsg WishDataActor.Fulfill(present.userId.get, present.product.productId)
      success
    }
    "call changeWishesCounters ONLY when the wish has been fulfilled" >> {
      def fixture(fulfilledWish: Option[Wish]) = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override def tryToFulfillWish(present: Present) = Future successful fulfilledWish
          override def changeWishesCounters(wish: Wish): Unit = probe.ref ! "changeWishesCounters"
        }
      }
      "call changeWishesCounters when the wish has been fulfilled" >> {
        val f = fixture(fulfilledWish = Some(null))
        import f._
        underlyingWriteActor.fulfillWish(present)
        probe expectMsg "changeWishesCounters"
        success
      }
      "do NOT call changeWishesCounters when the wish has NOT been fulfilled" >> {
        val f = fixture(fulfilledWish = None)
        import f._
        underlyingWriteActor.fulfillWish(present)
        probe expectNoMsg ()
        success
      }
    }
    "increment counters when new present has given" >> {
      def fixture(productTypeId: ProductTypeId) = new PresentActorWriteFixture {
        override def newPresent = PresentFixture.present(productTypeId = productTypeId)
        val updateStatisticActorProbe, productDataActorProbe = TestProbe()
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val productDataActor = productDataActorProbe.ref
          override lazy val wishDataActor = probe.ref
          override lazy val updateStatisticActor = updateStatisticActorProbe.ref
        }
      }
      "increment counters when newProductGiven" >> {
        val f = fixture(ProductTypeId.Product)
        import f._
        underlyingWriteActor.incrementPresentsCount(present)
        val userId = present.userId.get
        productDataActorProbe expectMsg ProductDataActor.IncreasePresentCounter(present.product.productId, +1)
        updateStatisticActorProbe.expectMsgAllOf(
          UpdateStatisticActor.ChangeUserNewPresentsCounter(userId, 1),
          UpdateStatisticActor.ChangeUserPresentsCounter(userId, 1),
          UpdateStatisticActor.ChangeUserSentPresentsCounter(present.senderId, 1)
        )
        updateStatisticActorProbe expectNoMsg ()
        success
      }
      "increment counters when newCertificateGiven" >> {
        val f = fixture(ProductTypeId.Certificate)
        import f._
        underlyingWriteActor.incrementPresentsCount(present)
        val userId = present.userId.get
        productDataActorProbe expectMsg ProductDataActor.IncreasePresentCounter(present.product.productId, +1)
        updateStatisticActorProbe.expectMsgAllOf(
          UpdateStatisticActor.ChangeUserNewPresentsCounter(userId, 1),
          UpdateStatisticActor.ChangeUserCertificatesCounter(userId, 1),
          UpdateStatisticActor.ChangeUserSentCertificatesCounter(present.senderId, 1)
        )
        updateStatisticActorProbe expectNoMsg ()
        success
      }
    }
    "increment sales counter" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val updateStatisticActor = probe.ref
        }
      }
      import f._
      underlyingWriteActor.incrementSalesCount(present)
      probe.expectMsgAllOf(
        UpdateStatisticActor.ChangeSalesCounter(present.product.locationId, 1)
      )
      success
    }
    "getPayment" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val paymentDataActor = probe.ref
        }
      }
      import f._
      underlyingWriteActor.getPayment(OrderId)
      probe expectMsg PaymentDataActor.GetPayment(OrderId)
      success
    }
    "savePresent" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val presentDataActor = probe.ref
        }
      }
      import f._
      underlyingWriteActor.savePresent(present)
      probe expectMsg PresentDataActor.Save(present)
      success
    }
    "addGift" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val userDataActor = probe.ref
        }
      }
      import f._
      underlyingWriteActor.addGift(present)
      probe expectMsg UserDataActor.AddGift(present.senderId, present.id)
      success
    }
    "createUserEvent" >> {
      val f = new PresentActorWriteFixture {
        def presentWriteActorFactory = new PresentWriteActor {
          override lazy val userEventWriteActor = probe.ref
        }
      }
      import f._
      underlyingWriteActor.createUserEvent(present)
      probe expectMsg UserEventWriteActor.NewPresentReceived(present)
      success
    }
  }

  "PresentWriteActor.createPresent" should {
    val f = new PresentActorWriteFixture {
      def presentWriteActorFactory = new PresentWriteActor
    }
    import f._
    val orderId = BigInt(1)

    "fill anonymousRecipient if recipient is an anonymous user" >> {
      val pf = new PaymentFixture(recipientId = None)
      val recipient = underlyingWriteActor.createPresent(orderId)(pf.payment).anonymousRecipient.get
      recipient.email.get === pf.recipient.email
      recipient.phone === pf.recipient.getPhoneShort
    }
    "NOT fill anonymousRecipient if recipient ID is known" >> {
      val pf = new PaymentFixture(recipientId = Some(UserId()))
      underlyingWriteActor.createPresent(orderId)(pf.payment).anonymousRecipient === None
    }
  }

  step {
    system.shutdown()
  }
}

abstract class PresentActorWriteFixture(implicit system: ActorSystem) {
  val probe = TestProbe()

  val OrderId: Payment.OrderId = BigInt(100)
  val payment = new PaymentFixture().payment
  val recipient = payment.recipient
  val productSnapshot = payment.productSnapshot

  def newPresent = PresentFixture.present()

  val present = newPresent

  def presentWriteActorFactory: PresentWriteActor

  lazy val presentWriteActor = TestActorRef[PresentWriteActor](Props(presentWriteActorFactory))

  def underlyingWriteActor = presentWriteActor.underlyingActor
}