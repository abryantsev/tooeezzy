package com.tooe.core.db.graph.test

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.tooe.core.db.graph.GraphGetFriendsActor
import com.tooe.core.db.graph.GraphPutFriendsActor
import com.tooe.core.db.graph.UserGraphActor
import com.tooe.core.db.graph.msg.GraphFriendship
import com.tooe.core.db.graph.msg.GraphPutFriends
import com.tooe.core.db.graph.msg.GraphPutUser
import com.tooe.core.test.TestSuite
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.tooe.core.db.graph.msg.GraphPutUserGroups
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit.SECONDS
import com.tooe.core.db.graph.msg.GraphGetFriendship
import com.tooe.core.domain.{UserGroupType, UserId}
import org.bson.types.ObjectId
import org.junit.Ignore
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class FriendBugfixTempSuiteV1 extends TestSuite {

  sequential
  step(setUp)

  val userId1, userId2 = UserId(new ObjectId())
  val permutations = UserGroupType.values.permutations.toList.flatMap(xs => xs.tails).distinct

  val groups = (Nil :: Random.shuffle(permutations), Nil :: Random.shuffle(permutations))

  override implicit val timeout = Timeout(100000)
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val graphPutUserActor = TestActorRef[UserGraphActor]
  lazy val graphPutFriendsActor = TestActorRef[GraphPutFriendsActor]
  lazy val graphGetFriendsActor = TestActorRef[GraphGetFriendsActor]

  "Graph actors" should {
    "put new users" in {
      val result = for {
        u1 <- graphPutUserActor ? new GraphPutUser(userId1)
        u2 <- graphPutUserActor ? new GraphPutUser(userId2)
      } yield (u1, u2)

      Await.result(result, Duration(2, SECONDS))

      success
    }
  }

  "Graph actors" should {
    "update usergroups correct" in {
      val result = graphPutFriendsActor ? new GraphPutFriends(userId1, userId2)

      Await.ready(result, Duration(10, SECONDS))

      val iter = groups._1.zip(groups._2).iterator

      while (iter.hasNext) {
        val (g1, g2) = iter.next()

        val whack = for {
          f <- saveUserGroups(userId1, userId2, g1)
          s <- saveUserGroups(userId2, userId1, g2)
        } yield (f, s)

        val res = Await.result(whack, Duration(2, SECONDS))
        if (true) println(res)
      }

      success
    }
  }

  "Graph actor" should {
    "get correct friendship" in {
      val future: Future[GraphFriendship] = (graphGetFriendsActor ? new GraphGetFriendship(userId1, userId2)).mapTo[GraphFriendship]
      val result = Await.result(future, Duration(2, SECONDS))
      val ug = result.getUsergroups

      result.getFriends.contains(userId1) should beTrue
      result.getFriends.contains(userId2) should beTrue
      groups._1.last.forall(lug => ug.contains(lug.toFriendshipType)) should beTrue

      success
    }
  }

  def saveUserGroups(userIdA: UserId, userIdB: UserId, userGroups: Seq[UserGroupType]) =
    graphPutFriendsActor.ask(new GraphPutUserGroups(userIdA, userIdB, userGroups.toFriendshipType.toArray))

  override def getTestContext(): String = {
    "/test/titan-graph-ctxt.xml"
  }
}
