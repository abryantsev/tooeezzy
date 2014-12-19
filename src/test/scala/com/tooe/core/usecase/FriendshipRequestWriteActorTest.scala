package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem}
import com.tooe.core.db.mongo.domain._
import concurrent.Future
import com.tooe.core.domain.{FriendshipRequestId, UserEventStatus, UserEventId, UserId}
import com.tooe.core.service.{FriendshipRequestFixture, UserEventFixture}
import com.tooe.core.util.Lang

class FriendshipRequestWriteActorTest extends ActorTestSpecification {

  "FriendshipRequestWriteActor" should {

    "send ChangeFriendshipRequestCounter on OfferFriendship request" >> {
      val f = new FriendshipRequestWriteActorFixture {
        def friendActorFactory = new FriendshipRequestWriteActor {
          override lazy val updateStatisticActor = updateStatisticProbe.ref
          override def getUsers(userId: UserId, currentUserId: UserId) = Future successful Seq(user1, user2)

          override def preventUserSendFriendshipRequestToStar(userId: UserId, actorId: UserId, lang: Lang) =
            Future successful ()

          override def saveFriendshipRequest(userId: UserId, actorId: UserId) =
            Future successful new FriendshipRequestFixture().friendshipRequest

          override def findFriendshipRequest(userId: UserId, actorId: UserId) = Future successful None

          override def isFriends(currentUserId: UserId, userId: UserId) = Future.successful(false)
        }
      }
      import f._

      friendshipRequestWriteActor ! FriendshipRequestWriteActor.OfferFriendship(user1.id, user2.id, null)
      updateStatisticProbe expectMsg UpdateStatisticActor.ChangeFriendshipRequestCounter(user1.id, 1)
      success
    }
    "send ChangeFriendshipRequestCounter on AcceptOrRejectFriendship" >> {
      val f = new FriendshipRequestWriteActorFixture {
        val userEvent = new UserEventFixture(userId = user1.id, actorId = user2.id).userEvent

        def friendActorFactory = new FriendshipRequestWriteActor {
          override lazy val updateStatisticActor = updateStatisticProbe.ref

          override def friendshipRequestReply(fr: FriendshipRequestId, ue: Option[UserEventId], status: UserEventStatus) =
            Future successful Some(userEvent)

          override def checkFriendshipRequest(friendshipRequest: FriendshipRequest, userId: UserId) =
            Future successful ()

          override def addFriendship(friend1: UserId, friend2: UserId) = Future successful null

          override def getFriendshipRequest(id: FriendshipRequestId) =
            Future successful new FriendshipRequestFixture(actorId = user2.id, userId = user1.id).friendshipRequest

          override def isFriends(currentUserId: UserId, userId: UserId) = Future.successful(false)
        }
      }
      import f._

      val friendshipRequestId = FriendshipRequestId()
      val request = AcceptOrRejectFriendshipRequest(UserEventStatus.Confirmed, Some(userEvent.id))
      friendshipRequestWriteActor ! FriendshipRequestWriteActor.AcceptOrRejectFriendship(request, friendshipRequestId, user1.id)
      updateStatisticProbe expectMsg UpdateStatisticActor.ChangeFriendshipRequestCounter(user1.id, -1)
      success
    }
  }

  step {
    system.shutdown()
  }
}

abstract class FriendshipRequestWriteActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {

  val user1, user2 = new UserFixture().user
  val updateStatisticProbe = TestProbe()

  def friendActorFactory: FriendshipRequestWriteActor

  lazy val friendshipRequestWriteActor = TestActorRef[FriendshipRequestWriteActor](Props(friendActorFactory))
}