package com.tooe.core.usecase

import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.actor.{Props, ActorSystem}
import com.tooe.core.db.mongo.domain._
import concurrent.Future
import com.tooe.core.domain.UserId
import com.tooe.core.db.graph.msg.GraphFriendship
import scala.collection.JavaConverters._
import com.tooe.core.usecase.session.CacheUserOnlineDataActor

class FriendWriteActorTest extends ActorTestSpecification {

  "FriendWriteActor" should {
    "send ChangeFriendshipCounters on AddFriendship" >> {
      val f = new FriendWriteActorFixture {
        val updateStatisticProbe = TestProbe()

        def friendWriteActorFactory = new FriendWriteActor {
          override lazy val updateStatisticActor = updateStatisticProbe.ref
          override def putFriendsToGraph(friend1: UserId, friend2: UserId) =
            Future successful new GraphFriendship(userIds.asJavaCollection)
        }
      }
      import f._
      friendWriteActor ! FriendWriteActor.AddFriendship(user2.id, user1.id)
      updateStatisticProbe expectMsg UpdateStatisticActor.ChangeFriendshipCounters(userIds, +1)
      success
    }
    "synchronize CacheUserOnline.friends on" >> {
      def fixture = new FriendWriteActorFixture {
        val probe = TestProbe()
        def friendWriteActorFactory = new FriendWriteActor {

          override lazy val cacheUserOnlineDataActor = probe.ref

          override def putFriendsToGraph(friend1: UserId, friend2: UserId) =
            Future successful new GraphFriendship(userIds.asJavaCollection)

          override def deleteFriendshipFromGraph(friend1: UserId, friend2: UserId) =
            Future successful true
        }
      }
      "AddFriendship" >> {
        val f = fixture
        import f._
        friendWriteActor ! FriendWriteActor.AddFriendship(user1.id, user2.id)
        probe expectMsgAllOf (
          CacheUserOnlineDataActor.AddFriend(user1.id, user2.id),
          CacheUserOnlineDataActor.AddFriend(user2.id, user1.id)
        )
        success
      }
      "DeleteFriendship" >> {
        val f = fixture
        import f._
        friendWriteActor ! FriendWriteActor.DeleteFriendship(user1.id, user2.id)
        probe expectMsgAllOf (
          CacheUserOnlineDataActor.RemoveFriend(user1.id, user2.id),
          CacheUserOnlineDataActor.RemoveFriend(user2.id, user1.id)
          )
        success
      }
    }
  }
}

abstract class FriendWriteActorFixture(implicit actorSystem: ActorSystem) extends TestKit(actorSystem) {
  val user1, user2 = new UserFixture().user
  val userIds = Set(user1.id, user2.id)

  def friendWriteActorFactory: FriendWriteActor

  lazy val friendWriteActor = TestActorRef[FriendWriteActor](Props(friendWriteActorFactory))
}