package com.tooe.core.db.graph.test

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.tooe.core.db.graph.msg.GraphPutFavorite
import com.tooe.core.db.graph.msg.GraphFavorite
import com.tooe.core.db.graph.msg.GraphPutUser
import com.tooe.core.db.graph.msg.GraphPutLocationAcknowledgement
import com.tooe.core.db.graph.msg.GraphPutUserAcknowledgement
import com.tooe.core.db.graph.test.data.TestDataFavorites
import com.tooe.core.db.graph.test.data.TestDataFriends
import com.tooe.core.test.TestSuite
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import com.tooe.core.db.graph.msg.GraphDeleteUser
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit.SECONDS
import com.tooe.core.db.graph.msg.GraphLocations
import com.tooe.core.db.graph.msg.GraphDeleteFavorite
import com.tooe.core.db.graph._
import com.tooe.api.service.ObjectId
import com.tooe.core.domain.LocationId
import com.tooe.core.domain.UserId
import com.tooe.core.db.graph.msg.GraphPutFriends
import com.tooe.core.db.graph.msg.GraphFriendship

@RunWith(classOf[JUnitRunner])
class FavoritesTestSuite extends TestSuite with TestDataFriends with TestDataFavorites {

  sequential

  override implicit val timeout = Timeout(100000)

  lazy val graphPutLocationActor = TestActorRef[LocationGraphActor]
  lazy val graphPutFavoritesActor = TestActorRef[GraphPutFavoritesActor]
  lazy val graphPutUserActor = TestActorRef[UserGraphActor]
  lazy val graphGetFavoritesActor = TestActorRef[GraphGetFavoritesActor]
  lazy val graphPutFriendsActor = TestActorRef[GraphPutFriendsActor]

  import LocationGraphActor._
  import TestKeysConverter._
  import LogHelper._
  import TestHelper._
  step(setUp)

  "Graph actors for location" should {

    "put new locations and users" in {
      val futurePut1u = graphPutUserActor ? new GraphPutUser(userId1)
      val result1u = Await.result(futurePut1u.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS))
      result1u mustEqual new GraphPutUserAcknowledgement(userId1)

      val futurePut2u = graphPutUserActor ? new GraphPutUser(userId2)    
      val result2u = Await.result(futurePut2u.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS)) 
      result2u mustEqual new GraphPutUserAcknowledgement(userId2)

      val futurePut12 = graphPutFriendsActor ? new GraphPutFriends(userId1, userId2)    
      val result12 = Await.result(futurePut12.mapTo[GraphFriendship], Duration(2, SECONDS)) 
      result12 mustEqual new GraphFriendship(friends12)
      
      val futurePut1 = graphPutLocationActor ? new GraphPutLocation(locationId1)
      val result1 = Await.result(futurePut1.mapTo[GraphPutLocationAcknowledgement], Duration(2, SECONDS))
      result1 mustEqual new GraphPutLocationAcknowledgement(locationId1)

      val futurePut2 = graphPutLocationActor ? new GraphPutLocation(locationId2)
      val result2 = Await.result(futurePut2.mapTo[GraphPutLocationAcknowledgement], Duration(2, SECONDS))
      result2 mustEqual new GraphPutLocationAcknowledgement(locationId2)

      val futurePut2a = graphPutLocationActor ? new GraphPutLocation(locationId2)
      Await.result(futurePut2a, Duration(2, SECONDS)) must throwA[GraphException]

      success
    }

    "put favorites for locations" in {
      val futurePut11 = graphPutFavoritesActor ? new GraphPutFavorite(userId1, locationId1)
      val result11 = Await.result(futurePut11.mapTo[GraphFavorite], Duration(2, SECONDS))
      result11 mustEqual new GraphFavorite(userId1, locationId1)

      val futurePut12 = graphPutFavoritesActor ? new GraphPutFavorite(userId1, locationId2)
      val result12 = Await.result(futurePut12.mapTo[GraphFavorite], Duration(2, SECONDS))
      result12 mustEqual new GraphFavorite(userId1, locationId2)

      success
    }

    "put favorites if already exists" in {
      val futurePut2 = graphPutFavoritesActor ? new GraphPutFavorite(userId1, locationId1)
      Await.result(futurePut2, Duration(2, SECONDS)) must throwA[GraphException]

      success
    }

    "put favorites for not existent user" in {
      val futurePutX = graphPutFavoritesActor ? new GraphPutFavorite(UserId(ObjectId()), locationId1)
      Await.result(futurePutX, Duration(2, SECONDS)) must throwA[GraphException]

      success
    }

    "put favorites for not existent location" in {
      val futurePutX = graphPutFavoritesActor ? new GraphPutFavorite(userId1, LocationId(ObjectId()))
      Await.result(futurePutX, Duration(2, SECONDS)) must throwA[GraphException]

      success
    }

    "get favorites list for user" in {
      val future = graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId1)
      val result = Await.result(future.mapTo[GraphLocations], Duration(2, SECONDS))
      result mustEqual new GraphLocations(favorites12)

      success
    }
    
    "get favorites list for user friends" in {
      val future = graphGetFavoritesActor ? new GraphGetFriendsFavoriteLocations(userId2)
      val result = Await.result(future.mapTo[GraphLocations], Duration(2, SECONDS))
      result mustEqual new GraphLocations(favorites12)

      success
    }
    
    "delete favorites for user" in {
//      mustEqualChecker(graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId1))( new GraphLocations(favorites12))
//      mustEqualChecker(graphPutFavoritesActor ? new GraphDeleteFavorite(userId1, locationId2))( true)
      val future = graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId1)
      val result = Await.result(future.mapTo[GraphLocations], Duration(2, SECONDS))
      result mustEqual new GraphLocations(favorites12)
           
      val future1 = graphPutFavoritesActor ? new GraphDeleteFavorite(userId1, locationId2)
      val result1 = Await.result(future1.mapTo[Boolean], Duration(2, SECONDS))
      result1 mustEqual true

      val future2 = graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId1)
      val result2 = Await.result(future2.mapTo[GraphLocations], Duration(2, SECONDS))
      result2 mustEqual new GraphLocations(favorites1)
      
      val future3 = graphPutFavoritesActor ? new GraphDeleteFavorite(userId1, locationId1)
      val result3 = Await.result(future3.mapTo[Boolean], Duration(2, SECONDS))
      result3 mustEqual true
      
      val future4 = graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId1)
      val result4 = Await.result(future4.mapTo[GraphLocations], Duration(2, SECONDS))
      result4 mustEqual new GraphLocations(emptyFavorites)
      
//      mustEqualLoopChecker(graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId1))( new GraphLocations(favorites1)).logTimeWithMessage("after delete (userId1, locationId2)")
//      mustEqualChecker(graphPutFavoritesActor ? new GraphDeleteFavorite(userId1, locationId1))( true)
//      mustEqualLoopChecker(getFavoriteLocations(userId1))( new GraphLocations(emptyFavorites)).logTimeWithMessage("after (userId1, locationId1)")

      success
    }

    "delete users and locations" in {
      val futureDelete1 = graphPutUserActor ? new GraphDeleteUser(userId1)
      val resultDelete1 = Await.result(futureDelete1, Duration(22, SECONDS))
      resultDelete1 mustEqual true

      val futureDelete1a = graphPutLocationActor ? new GraphDeleteLocation(locationId1)
      val resultDelete1a = Await.result(futureDelete1a, Duration(22, SECONDS))
      resultDelete1a mustEqual true

      val futureDelete1b = graphPutLocationActor ? new GraphDeleteLocation(locationId2)
      val resultDelete1b = Await.result(futureDelete1b, Duration(22, SECONDS))
      resultDelete1b mustEqual true

      success
    }

    /*"isFavorite location check" in {
      val futurePut1u = graphPutUserActor ? new GraphPutUser(userId1)
      val result1u = Await.result(futurePut1u.mapTo[GraphPutUserAcknowledgement], Duration(2, SECONDS))
      result1u mustEqual new GraphPutUserAcknowledgement(userId1)

      val futurePutLocation = graphPutLocationActor ? new GraphPutLocation(testLocationId)
      val resultLocation = Await.result(futurePutLocation.mapTo[GraphPutLocationAcknowledgement], Duration(2, SECONDS))
      resultLocation mustEqual new GraphPutLocationAcknowledgement(testLocationId)

      val futurePutFavorite = graphPutFavoritesActor ? new GraphPutFavorite(userId1, testLocationId)
      val resultFavorite = Await.result(futurePutFavorite.mapTo[GraphFavorite], Duration(20, SECONDS))
      resultFavorite mustEqual new GraphFavorite(userId1, testLocationId)

      val future1 = graphGetFavoritesActor ? new IsFavoriteLocation(userId1, testLocationId)
      val result1 = Await.result(future1.mapTo[Boolean], Duration(2, SECONDS))
      result1 mustEqual true

      success
    }*/

  }

  step(tearDown)

  override def getTestContext(): String = {
    "/test/titan-graph-ctxt.xml"
  }

  def getFavoriteLocations(userId: UserId): GraphLocations = {
    val future = graphGetFavoritesActor ? new GraphGetFavoriteLocations(userId)
    Await.result(future.mapTo[GraphLocations], Duration(2, SECONDS))
  }


}


