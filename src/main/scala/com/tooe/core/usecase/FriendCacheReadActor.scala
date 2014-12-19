package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.api.service.ExecutionContextProvider
import com.tooe.core.usecase.friends_cache.FriendCacheDataActor
import com.tooe.core.domain.UserId
import com.tooe.core.db.graph.domain.FriendshipType
import scala.concurrent.Future
import com.tooe.core.usecase.friends_cache.FriendCacheDataActor.{Save, Find}
import com.tooe.core.db.graph.msg.{GraphFriends, GraphGetFriends}
import com.tooe.core.db.graph.GraphGetFriendsActor
import scala.collection.JavaConverters._

object FriendCacheReadActor {
  final val Id = Actors.FriendReadCache

  case class GetUserFriends(currentUserId: UserId, usersGroup: FriendshipType)
}

trait FriendCacheReadComponent { self: AppActor with ExecutionContextProvider =>
  lazy val friendCacheReadActor = lookup(FriendCacheReadActor.Id)

  import FriendCacheReadActor._

  def getUserFriendsViaCache(currentUserId: UserId, usersGroup: FriendshipType): Future[Seq[UserId]] =
    (friendCacheReadActor ? GetUserFriends(currentUserId, usersGroup)).mapTo[Seq[UserId]]

}

class FriendCacheReadActor extends AppActor with ExecutionContextProvider {

  lazy val friendCacheDataActor = lookup(FriendCacheDataActor.Id)
  lazy val getFriendGraphActor = lookup(GraphGetFriendsActor.Id)

  import FriendCacheReadActor._

  def receive = {
    case GetUserFriends(currentUserId, usersGroup) =>
      (friendCacheDataActor ? Find(currentUserId, usersGroup)).mapTo[Option[Seq[UserId]]].flatMap { friendFromCacheIds =>
        friendFromCacheIds map Future.successful getOrElse getFriendsFromGraphFtr(currentUserId, usersGroup)
      } pipeTo sender
  }

  def getFriendsFromGraphFtr(currentUserId: UserId, friendsGroup: FriendshipType) = getFriends(currentUserId, friendsGroup) map { friends =>
    friendCacheDataActor ! Save(currentUserId, friendsGroup, friends)
    friends
  }

  def getFriends(userId: UserId, friendGroup: FriendshipType): Future[Seq[UserId]] =
    (getFriendGraphActor ? new GraphGetFriends(userId, friendGroup)).mapTo[GraphFriends] map { graphFriends =>
      graphFriends.getFriends.asScala.toSeq
    }
}