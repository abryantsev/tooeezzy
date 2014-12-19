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

@Ignore
@RunWith(classOf[JUnitRunner])
class TitanTestSuite extends TestSuite with TestDataFriends {

  sequential

  override implicit val timeout = Timeout(100000)      

  lazy val graphPutUserActor = TestActorRef[UserGraphActor]
  lazy val graphPutFriendsActor = TestActorRef[GraphPutFriendsActor]
  lazy val graphGetFriendsActor = TestActorRef[GraphGetFriendsActor]

  import TestKeysConverter._

  step(setUp)

  "Titan " should {

    "put new users" in {
      val futurePut1 = graphPutUserActor ? new GraphPutUser(userId1)    
      val result1 = Await.result(futurePut1.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS)) 
      result1 mustEqual new GraphPutUserAcknowledgement(userId1)
      
      val futurePut2 = graphPutUserActor ? new GraphPutUser(userId2)    
      val result2 = Await.result(futurePut2.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS)) 
      result2 mustEqual new GraphPutUserAcknowledgement(userId2)

      success
    }

    "put friends for user1 & user2" in {
      val futurePut12 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId2)    
      val result12 = Await.result(futurePut12.mapTo[GraphFriendship], Duration(2, SECONDS)) 
      result12 mustEqual new GraphFriendship(friends12)
      
      success
    }

    "check friendship for users" in {
      val future = graphGetFriendsActor ? new GraphCheckFriends(userId1, userId2)   
      val result = Await.result(future.mapTo[Boolean], Duration(2, SECONDS)) 
      result mustEqual true

      success
    }

    "delete friends for user1 & user2" in {
      println("delete friends for user1 & user2 .........")
      val futureDelete1 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId2)
      val resultDelete1 = Await.result(futureDelete1, Duration(2, SECONDS)) 
      println("resultDelete1: "+resultDelete1)
      resultDelete1 mustEqual true
//      var i = 0;
//      var found = false;
//      while(!found && i < 10) {
//        println("i.....:"+i)
//	      val futureDelete1 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId2)
//	      val resultDelete1 = Await.result(futureDelete1.mapTo[Boolean], Duration(2, SECONDS)) 
//	      println("resultDelete1: "+resultDelete1)
//	      found = resultDelete1;
//        i=i+1;
//      }

      success
    }

    "check friendship for users" in {
      val future = graphGetFriendsActor ? new GraphCheckFriends(userId1, userId2)   
      val result = Await.result(future.mapTo[Boolean], Duration(2, SECONDS)) 
      result mustEqual false

      success
    }
    
    "delete all friends for user1" in {
      val futureDelete1 = graphPutUserActor ? new GraphDeleteUser(userId1)
      val resultDelete1 = Await.result(futureDelete1, Duration(2, SECONDS)) 
      resultDelete1 mustEqual true

      val futureDelete2 = graphPutUserActor ? new GraphDeleteUser(userId2)
      val resultDelete2 = Await.result(futureDelete2, Duration(2, SECONDS)) 
      resultDelete2 mustEqual true

      success
    }    
    
  }

  step(tearDown)

  override def getTestContext() : String = {
    "/test/titan-graph-ctxt.xml"
  }
  
}

