
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
import org.slf4j.LoggerFactory
import akka.event.Logging
import org.slf4j.Logger
import com.tooe.core.db.graph.msg.GraphGetMutualFriends

@RunWith(classOf[JUnitRunner])
class FriendsTestSuite extends TestSuite with TestDataFriends {

  sequential

  override implicit val timeout = Timeout(100000)      

  lazy val graphPutUserActor = TestActorRef[UserGraphActor]
  lazy val graphPutFriendsActor = TestActorRef[GraphPutFriendsActor]
  lazy val graphGetFriendsActor = TestActorRef[GraphGetFriendsActor]
  
  lazy val logger = LoggerFactory.getLogger(getClass)
  
  import TestKeysConverter._

  step(setUp)

  "Graph actors for user" should {

    "put new users" in {
    	println("userId1: "+userId1)
    	println("userId2: "+userId2)
    	println("userId3: "+userId3)
      
      val futurePut1 = graphPutUserActor ? new GraphPutUser(userId1)    
      val result1 = Await.result(futurePut1.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS)) 
      result1 mustEqual new GraphPutUserAcknowledgement(userId1)
      
      val futurePut2 = graphPutUserActor ? new GraphPutUser(userId2)    
      val result2 = Await.result(futurePut2.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS)) 
      result2 mustEqual new GraphPutUserAcknowledgement(userId2)

      val futurePut2a = graphPutUserActor ? new GraphPutUser(userId2)    
      Await.result(futurePut2a, Duration(2, SECONDS)) must throwA[GraphException]

      val futurePut3 = graphPutUserActor ? new GraphPutUser(userId3)    
      val result3 = Await.result(futurePut3.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS)) 
      result3 mustEqual new GraphPutUserAcknowledgement(userId3)

      success
    }

    "put friends for user1 & user2" in {
      val futurePut12 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId2)    
      val result12 = Await.result(futurePut12.mapTo[GraphFriendship], Duration(2, SECONDS)) 
      result12 mustEqual new GraphFriendship(friends12)
      
      val futurePut13 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId3)    
      val result13 = Await.result(futurePut13.mapTo[GraphFriendship], Duration(2, SECONDS)) 
      result13 mustEqual new GraphFriendship(friends13)

      success
    }

    "put friends for not existent userX & userY" in {
      val futurePutXY = graphPutFriendsActor ? new GraphPutFriends(notExistingUser, notExistingUser)
      Await.result(futurePutXY, Duration(2, SECONDS)) must throwA[GraphException]
      
      success
    }

    "check friendship for users" in {
      val future = graphGetFriendsActor ? new GraphCheckFriends(userId1, userId2)   
      val result = Await.result(future.mapTo[Boolean], Duration(2, SECONDS)) 
      result mustEqual true

      success
    }

    "get friends list for user" in {
      val future = graphGetFriendsActor ? new GraphGetFriends(userId1, emptyGroup)   
      val result = Await.result(future.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result mustEqual new GraphFriends(friendslist1)

      success
    }

    "get friendslist with single friend for user" in {
      val future = graphGetFriendsActor ? new GraphGetFriends(userId3, emptyGroup)   
      val result = Await.result(future.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result mustEqual new GraphFriends(friendslist3)

      success
    }

    "get mutual friends" in {
      val future = graphGetFriendsActor ? new GraphGetMutualFriends(userId2, userId3)   
      val result = Await.result(future.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result mustEqual new GraphFriends(mutualFriendslist)

      success
    }
    
    "put usergroups for user1 & user2" in {
      val futurePutUsergroups12 = graphPutFriendsActor ? new GraphPutUserGroups(userId1, userId2, usergroups12)
      val resultUsergroups12 = Await.result(futurePutUsergroups12, Duration(2, SECONDS)) 
      resultUsergroups12 mustEqual new GraphFriendship(friends12, usergroups12asList)

      val futurePutUsergroups21 = graphPutFriendsActor ? new GraphPutUserGroups(userId2, userId1, usergroups13)
      val resultUsergroups21 = Await.result(futurePutUsergroups21, Duration(2, SECONDS)) 
      resultUsergroups21 mustEqual new GraphFriendship(friends12, usergroups13asList)

      val futurePutUsergroups13 = graphPutFriendsActor ? new GraphPutUserGroups(userId1, userId3, usergroups13)
      val resultUsergroups13 = Await.result(futurePutUsergroups13, Duration(2, SECONDS)) 
      resultUsergroups13 mustEqual new GraphFriendship(friends13, usergroups13asList)

      success
    }

    "get friendship" in {
      val future1 = graphGetFriendsActor ? new GraphGetFriendship(userId1, userId2)   
      val result1 = Await.result(future1.mapTo[GraphFriendship], Duration(2, SECONDS)) 
      result1 mustEqual new GraphFriendship(friends12, usergroups12asList)
    
      val future2 = graphGetFriendsActor ? new GraphGetFriendship(userId2, userId1)   
      val result2 = Await.result(future2.mapTo[GraphFriendship], Duration(2, SECONDS)) 
      result2 mustEqual new GraphFriendship(friends12, usergroups13asList)

      success
    }
    
    "get friends list for user and usergroup" in {
      val future1 = graphGetFriendsActor ? new GraphGetFriends(userId1, usergroup1)   
      val result1 = Await.result(future1.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result1 mustEqual new GraphFriends(usergroup1friendslist)

      val future2 = graphGetFriendsActor ? new GraphGetFriends(userId1, usergroup2)   
      val result2 = Await.result(future2.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result2 mustEqual new GraphFriends(usergroup2friendslist)

      success
    }

    "delete usergroups for user1 & user2" in {
      val futurePutUsergroups12 = graphPutFriendsActor ? new GraphPutUserGroups(userId1, userId2, usergroupsEmpty)
      val resultUsergroups12 = Await.result(futurePutUsergroups12, Duration(2, SECONDS)) 
      resultUsergroups12 mustEqual new GraphFriendship(friends12, usergroupsEmptyasList)

      success
    }

    "get friendship with empty usergroups" in {
      val future1 = graphGetFriendsActor ? new GraphGetFriendship(userId1, userId2)   
      val result1 = Await.result(future1.mapTo[GraphFriendship], Duration(2, SECONDS))
      
      logger.debug("get friendship with empty usergroups returns: "+result1.toString())
      
      result1 mustEqual new GraphFriendship(friends12, usergroupsEmptyasList)
    
      success
    }
    
    "check befor delete friends for user1 & user2" in {
      val future1 = graphGetFriendsActor ? new GraphGetFriends(userId1, emptyGroup)   
      val result1 = Await.result(future1.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result1 mustEqual new GraphFriends(friendslist1)

      val future2 = graphGetFriendsActor ? new GraphGetFriends(userId2, emptyGroup)   
      val result2 = Await.result(future2.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result2 mustEqual new GraphFriends(friendslist2)

      success
    }

    "delete friends for user1 & user2" in {
      val futureDelete1 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId2)
      val resultDelete1 = Await.result(futureDelete1, Duration(22, SECONDS)) 
      resultDelete1 mustEqual true
      
      val future1a = graphGetFriendsActor ? new GraphGetFriends(userId1, emptyGroup)   
      val result1a = Await.result(future1a.mapTo[GraphFriends], Duration(2, SECONDS))
      
      logger.debug("delete friends for user1 & user2 returns: "+result1a.toString())
      
      result1a mustEqual new GraphFriends(friendslist1a)

      val future2a = graphGetFriendsActor ? new GraphGetFriends(userId2, emptyGroup)   
      val result2a = Await.result(future2a.mapTo[GraphFriends], Duration(2, SECONDS)) 
      result2a mustEqual new GraphFriends(emptyFriends)

      success
    }
   
    "add usergroups to not existent friendship" in {
//      val futureDelete1 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId2)
//      val resultDelete1 = Await.result(futureDelete1, Duration(2, SECONDS)) 
//      resultDelete1 mustEqual true
      
      val futurePutUsergroups12a = graphPutFriendsActor ? new GraphPutUserGroups(userId1, userId22, usergroups12)
      Await.result(futurePutUsergroups12a, Duration(2, SECONDS))  must throwA[GraphException]
      
      success
    }

    
    //instable after refactoring of TestDataFriends!!
    
//    "delete all friends for user1" in {
//      val futurePut12 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId2)    
//      val result12 = Await.result(futurePut12.mapTo[GraphFriendship], Duration(2, SECONDS)) 
//      result12 mustEqual new GraphFriendship(friends12)
//
//      val futurePutUsergroups12a = graphPutFriendsActor ? new GraphPutUserGroups(userId1, userId2, usergroups12)
//      val resultUsergroups12a = Await.result(futurePutUsergroups12a.mapTo[GraphFriendship], Duration(2, SECONDS)) 
//      resultUsergroups12a mustEqual new GraphFriendship(friends12, usergroups12asList)
//
//      val future1 = graphGetFriendsActor ? new GraphGetFriends(userId1, emptyGroup)   
//      val result1 = Await.result(future1.mapTo[GraphFriends], Duration(2, SECONDS)) 
//      result1 mustEqual new GraphFriends(friendslist1)
//
//      val future2 = graphGetFriendsActor ? new GraphGetFriends(userId2, emptyGroup)   
//      val result2 = Await.result(future2.mapTo[GraphFriends], Duration(2, SECONDS)) 
//      result2 mustEqual new GraphFriends(friendslist2)
//
//      val future3 = graphGetFriendsActor ? new GraphGetFriends(userId3, emptyGroup)   
//      val result3 = Await.result(future3.mapTo[GraphFriends], Duration(2, SECONDS)) 
//      result3 mustEqual new GraphFriends(friendslist3)
//      
//      val futureDelete12 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId2)
//      val resultDelete12 = Await.result(futureDelete12, Duration(2, SECONDS)) 
//      resultDelete12 mustEqual true
//      
//      val futureDelete13 = graphPutFriendsActor ? new GraphDeleteFriends(userId1, userId3)
//      val resultDelete13 = Await.result(futureDelete13, Duration(2, SECONDS)) 
//      resultDelete13 mustEqual true
//
//      val future1a = graphGetFriendsActor ? new GraphGetFriends(userId1, emptyGroup)   
//      val result1a = Await.result(future1a.mapTo[GraphFriends], Duration(2, SECONDS)) 
//      result1a mustEqual new GraphFriends(emptyFriends)
//
//      val future2a = graphGetFriendsActor ? new GraphGetFriends(userId2, emptyGroup)   
//      val result2a = Await.result(future2a.mapTo[GraphFriends], Duration(2, SECONDS)) 
//      result2a mustEqual new GraphFriends(emptyFriends)
//
//      val future3a = graphGetFriendsActor ? new GraphGetFriends(userId3, emptyGroup)   
//      val result3a = Await.result(future3a.mapTo[GraphFriends], Duration(2, SECONDS)) 
//      result3a mustEqual new GraphFriends(emptyFriends)
//
//      val futureDelete1 = graphPutUserActor ? new GraphDeleteUser(userId1)
//      val resultDelete1 = Await.result(futureDelete1, Duration(2, SECONDS)) 
//      resultDelete1 mustEqual true
//
//      val futureDelete2 = graphPutUserActor ? new GraphDeleteUser(userId2)
//      val resultDelete2 = Await.result(futureDelete2, Duration(2, SECONDS)) 
//      resultDelete2 mustEqual true
//
//      val futureDelete3 = graphPutUserActor ? new GraphDeleteUser(userId3)
//      val resultDelete3 = Await.result(futureDelete3, Duration(2, SECONDS)) 
//      resultDelete3 mustEqual true
//
//      success
//    }    
  }

  step(tearDown)

  override def getTestContext() : String = {
    "/test/titan-graph-ctxt.xml"
  }
  
}