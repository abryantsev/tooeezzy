package com.tooe.core.usecase

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, TestKit}
import com.tooe.core.db.mongo.domain.{Wish, UserFixture}
import com.tooe.core.service.{WishFixture, ProductFixture}
import concurrent.Future
import com.tooe.core.domain.{ProductId, WishId, UserId}
import org.bson.types.ObjectId
import com.tooe.core.db.mongo.domain
import java.util.Date
import com.tooe.core.usecase.user.UserDataActor

class WishWriteActorTest extends ActorTestSpecification {

  "WishWriteActor" should {

    "send ChangeUsersWishesCounter on creating wish " >> {
      val f = new WishWriteActorBaseFixture {
        val product = new ProductFixture().product
        val request = NewWishRequest(product.id)
        def testActorFactory = new WishWriteActorUnderTest {
          override def getProduct(newWish: NewWishRequest): Future[domain.Product] = Future successful product
          override def saveWish(wish: Wish): Future[Wish] = Future successful wish
          override def getWishByProduct(userId: UserId, productId: ProductId) = Future.successful(None)
        }
      }
      import f._
      wishWriteActor ! WishWriteActor.MakeWish(request, user.id)
      updateStatisticProbe expectMsg UpdateStatisticActor.ChangeUsersWishesCounter(user.id, 1)
      val addNewWishMsg = userDataProbe.expectMsgType[UserDataActor.AddNewWish]
      addNewWishMsg.userId === user.id
      addNewWishMsg.wishId !== null
    }
    "send messages on deleting wish" >> {
      def fixture(fulfillmentDate: Option[Date]) = new WishWriteActorBaseFixture {
        val wish = new WishFixture().wish.copy(fulfillmentDate = fulfillmentDate)
        def testActorFactory = new WishWriteActorUnderTest {
          override def checkWishBelongsToCurrentUser(wishId: WishId, userId: UserId): Future[Wish] = Future successful wish
        }
      }
      "send ChangeUsersWishesCounter on deleting non-fulfilled wish " >> {
        val f = fixture(fulfillmentDate = None)
        import f._
        wishWriteActor ! WishWriteActor.DeleteWish(wish.id, user.id)
        updateStatisticProbe expectMsg UpdateStatisticActor.ChangeUsersWishesCounter(user.id, -1)
        userDataProbe expectMsg UserDataActor.RemoveWish(user.id, wish.id)
        success
      }
      "send ChangeUsersFulfilledWishesCounter on deleting fulfilled wish " >> {
        val f = fixture(fulfillmentDate = Some(new Date))
        import f._
        wishWriteActor ! WishWriteActor.DeleteWish(wish.id, user.id)
        updateStatisticProbe expectMsg UpdateStatisticActor.ChangeUsersFulfilledWishesCounter(user.id, -1)
        userDataProbe expectMsg UserDataActor.RemoveWish(user.id, wish.id)
        success
      }
    }
  }

  step {
    system.shutdown()
  }
}

abstract class WishWriteActorBaseFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user = new UserFixture().user
  val updateStatisticProbe, userDataProbe = TestProbe()

  class WishWriteActorUnderTest extends WishWriteActor {
    override lazy val updateStatisticActor = updateStatisticProbe.ref
    override lazy val userDataActor = userDataProbe.ref
  }

  def testActorFactory: WishWriteActorUnderTest

  lazy val wishWriteActor = TestActorRef[WishWriteActorUnderTest](Props(testActorFactory))
}