
package com.tooe.core.db.graph.test

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.tooe.core.db.graph.GraphGetFriendsActor
import com.tooe.core.db.graph.GraphPutFriendsActor
import com.tooe.core.db.graph.UserGraphActor
import com.tooe.core.db.graph.msg.GraphFriends
import com.tooe.core.db.graph.msg.GraphFriendship
import com.tooe.core.db.graph.msg.GraphPutFriends
import com.tooe.core.db.graph.msg.GraphPutUser
import com.tooe.core.db.graph.msg.GraphPutUserAcknowledgement
import com.tooe.core.db.graph.test.data.TestDataFriends
import com.tooe.core.test.TestSuite
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.tooe.core.db.graph.msg.GraphGetFriends
import com.tooe.core.db.graph.msg.GraphDeleteFriends
import com.tooe.core.db.graph.msg.GraphPutUserGroups
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit.SECONDS
import com.tooe.core.db.graph.GraphException
import com.tooe.core.db.graph.msg.GraphDeleteUser
import com.tooe.core.db.graph.msg.GraphCheckFriends
import org.junit.Ignore
import com.tooe.core.db.graph.msg.GraphGetFriendship

//@Ignore
@RunWith(classOf[JUnitRunner])
class GraphDeleteTestSuite extends TestSuite with TestDataFriends {

  sequential

  override implicit val timeout = Timeout(100000)      

  lazy val graphPutUserActor = TestActorRef[UserGraphActor]
  lazy val graphPutFriendsActor = TestActorRef[GraphPutFriendsActor]
  lazy val graphGetFriendsActor = TestActorRef[GraphGetFriendsActor]

  import TestKeysConverter._

  step(setUp)

  "Graph actors for user" should {

    "put new users" in {
      val futurePut1 = graphPutUserActor ? new GraphPutUser(userId1)    
      val result1 = Await.result(futurePut1.mapTo[GraphPutUserAcknowledgement], Duration(222, SECONDS)) 
      result1 mustEqual new GraphPutUserAcknowledgement(userId1)
      
      val futurePut2 = graphPutUserActor ? new GraphPutUser(userId2)    
      val result2 = Await.result(futurePut2.mapTo[GraphPutUserAcknowledgement], Duration(222, SECONDS)) 
      result2 mustEqual new GraphPutUserAcknowledgement(userId2)

      val futurePut2a = graphPutUserActor ? new GraphPutUser(userId2)    
      Await.result(futurePut2a, Duration(2, SECONDS)) must throwA[GraphException]

      val futurePut3 = graphPutUserActor ? new GraphPutUser(userId3)    
      val result3 = Await.result(futurePut3.mapTo[GraphPutUserAcknowledgement], Duration(222, SECONDS)) 
      result3 mustEqual new GraphPutUserAcknowledgement(userId3)

      success
    }

    "put friends for user1 & user2" in {
      val futurePut12 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId2)    
      val result12 = Await.result(futurePut12.mapTo[GraphFriendship], Duration(222, SECONDS)) 
      result12 mustEqual new GraphFriendship(friends12)
      
      val futurePut13 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId3)    
      val result13 = Await.result(futurePut13.mapTo[GraphFriendship], Duration(222, SECONDS)) 
      result13 mustEqual new GraphFriendship(friends13)

      success
    }

    "check friendship for users 12" in {
      val future = graphGetFriendsActor ? new GraphCheckFriends(userId1, userId2)   
      val result = Await.result(future.mapTo[Boolean], Duration(222, SECONDS)) 
      result mustEqual true

      success
    }

    "check friendship for users 13" in {
      val future = graphGetFriendsActor ? new GraphCheckFriends(userId1, userId3)   
      val result = Await.result(future.mapTo[Boolean], Duration(222, SECONDS)) 
      result mustEqual true

      success
    }

    "check friendship for users 23" in {
      val future = graphGetFriendsActor ? new GraphCheckFriends(userId2, userId3)   
      val result = Await.result(future.mapTo[Boolean], Duration(222, SECONDS)) 
      result mustEqual false

      success
    }
    
    "delete friends for user1 & user2" in {
      val futureDelete1 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId2)
      val resultDelete1 = Await.result(futureDelete1, Duration(222, SECONDS)) 
      resultDelete1 mustEqual true
      
      val future1a = graphGetFriendsActor ? new GraphGetFriends(userId1, emptyGroup)   
      val result1a = Await.result(future1a.mapTo[GraphFriends], Duration(222, SECONDS)) 
      result1a mustEqual new GraphFriends(friendslist1a)

      val future2a = graphGetFriendsActor ? new GraphGetFriends(userId2, emptyGroup)   
      val result2a = Await.result(future2a.mapTo[GraphFriends], Duration(222, SECONDS)) 
      result2a mustEqual new GraphFriends(emptyFriends)

      success
    }
    
  }

  step(tearDown)

  override def getTestContext() : String = {
    "/test/titan-graph-ctxt.xml"
  }
  
}