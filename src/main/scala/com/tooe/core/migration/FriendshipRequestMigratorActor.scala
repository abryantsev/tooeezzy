package com.tooe.core.migration

import akka.pattern.{ask, pipe}
import com.tooe.core.db.mongo.util.UnmarshallerEntity
import java.util.Date
import com.tooe.core.usecase.{UpdateStatisticActor, AppActor}
import scala.concurrent.Future
import org.bson.types.ObjectId
import com.tooe.core.migration.db.domain.{IdMapping, MappingCollection}
import com.tooe.core.domain.{FriendshipRequestId, UserId}
import com.tooe.core.migration.api.{DefaultMigrationResult, MigrationResponse}
import com.tooe.core.usecase.friendshiprequest.FriendshipRequestDataActor
import com.tooe.core.db.mongo.domain.FriendshipRequest

object FriendshipRequestMigratorActor {
  val Id = 'FriendshipRequestMigrator
  case class LegacyFriendRequest(legacyid: Int, actorid: Int, time: Date)
  case class LegacyFriendshipRequests(legacyid: Int, friendshiprequests: Seq[LegacyFriendRequest]) extends UnmarshallerEntity
}

class FriendshipRequestMigratorActor extends MigrationActor {

  import FriendshipRequestMigratorActor._

  def receive = {
    case lfrs: LegacyFriendshipRequests =>
      val future = for {
        uid <- lookupByLegacyId(lfrs.legacyid, MappingCollection.user).map(UserId)
        count <- saveRequests(uid, lfrs.friendshiprequests)
      } yield MigrationResponse(DefaultMigrationResult(lfrs.legacyid, new ObjectId(), "friendshiprequest_migrator"))
      future pipeTo sender
  }

  def saveRequests(recipient: UserId, requests: Seq[LegacyFriendRequest]): Future[Int] =
    getIdMappings(requests.map(_.actorid), MappingCollection.user).map(_ zip requests).mapInner {
      case (uid, req) =>
        val friendshiReqId = new ObjectId()
        updateStatisticActor ! UpdateStatisticActor.ChangeFriendshipRequestCounter(recipient, 1)
        idMappingDataActor ! IdMappingDataActor.SaveIdMapping(IdMapping(new ObjectId, MappingCollection.friendshipRequest, req.legacyid, friendshiReqId))
        friendRequestDataActor ? FriendshipRequestDataActor.Save(FriendshipRequest(FriendshipRequestId(friendshiReqId), recipient, UserId(uid), req.time))
    }.map(_.length)

  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)
  lazy val friendRequestDataActor = lookup(FriendshipRequestDataActor.Id)
}