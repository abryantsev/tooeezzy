package com.tooe.core.db.graph.test

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.tooe.core.db.graph.GraphGetFriendsActor
import com.tooe.core.db.graph.GraphPutFriendsActor
import com.tooe.core.db.graph.UserGraphActor
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
class FriendBugfixTempSuite extends TestSuite {

  sequential
  step(setUp)

  def user = UserId(new ObjectId())
  val users = (1 to 5) map (_ => user)

  val permutations = UserGroupType.values.permutations.toList.flatMap(xs => xs.tails).distinct :+ Nil

  def shuffled[A](xs: List[Seq[A]]): List[Seq[A]] = {
    Random.shuffle(xs)
  }

  override implicit val timeout = Timeout(10000)
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  lazy val graphPutUserActor = TestActorRef[UserGraphActor]
  lazy val graphPutFriendsActor = TestActorRef[GraphPutFriendsActor]
  lazy val graphGetFriendsActor = TestActorRef[GraphGetFriendsActor]

  "Graph actor" should {
    "put new users" in {
      val f = users.map {
        u =>
          Await.result({
            graphPutUserActor ? new GraphPutUser(u)
          }, Duration(2, SECONDS))
      }
      success
    }



    "put friendship between users" in {
      allOnce(users) {
        (u1, u2) =>
          graphPutFriendsActor ? new GraphPutFriends(u1, u2)
      }
      success
    }

    "update usergroups correct" in {
      val whack = eachOther(users) {
        (u1, u2) =>
          saveUserGroups(u1, u2, util.groups)
      }

      if (true) println(whack.mkString("\n"))
      success
    }

    "get correct friendship" in {

      eachOther(users) {
        (u1, u2) =>
          graphGetFriendsActor ? new GraphGetFriendship(u1, u2)
      }.length shouldEqual users.permutations.toList.map(_.take(2)).distinct.length
      //What the heck?
      success
    }
  }

  object util {
    var iter: Stream[Seq[UserGroupType]] = (1 to 50).toStream.flatMap(_ => shuffled(permutations))
    def groups = {
      val h = iter.head
      iter = iter.tail
      h
    }
  }

  def allOnce[A, B](xs: Seq[A])(f: (A, A) => Future[B]): Seq[B] = {
    xs.permutations.map(_.take(2).toSet).toList.distinct.map {
      pair =>
        val Seq(x1, x2) = pair.toList
        Await.result(f(x1, x2), Duration(3, SECONDS))
    }
  }

  def eachOther[A, B](xs: Seq[A])(f: (A, A) => Future[B]): Seq[B] =
    xs.permutations.map(_.take(2)).toList.distinct.map {
      pair =>
        val Seq(x1, x2) = pair
        Await.result(f(x1, x2), Duration(3, SECONDS))
    }

  def saveUserGroups(userIdA: UserId, userIdB: UserId, userGroups: Seq[UserGroupType]) =
    graphPutFriendsActor.ask(new GraphPutUserGroups(userIdA, userIdB, userGroups.toFriendshipType.toArray))

  override def getTestContext(): String = {
    "/test/titan-graph-ctxt.xml"
  }
}
