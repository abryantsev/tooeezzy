package com.tooe.core.migration

import com.tooe.core.db.mongo.util.UnmarshallerEntity
import com.tooe.core.usecase.UpdateStatisticActor
import com.tooe.core.db.graph.GraphPutFriendsActor
import com.tooe.core.migration.db.domain.MappingCollection
import com.tooe.core.domain.{UserGroupType, UserId}
import scala.concurrent.Future
import com.tooe.core.db.graph.msg.{GraphPutUserGroups, GraphFriendship, GraphPutFriends}
import com.tooe.core.migration.api.{FriendshipMigrationResult, MigrationResponse}

object FriendshipMigratorActor {

  final val Id = 'friendshipMigrator

  case class LegacyFriendship(legacyidA: Int, legacyidB: Int, usergroupsAB: Seq[Int], usergroupsBA: Seq[Int]) extends UnmarshallerEntity

}

class FriendshipMigratorActor extends MigrationActor {
  import FriendshipMigratorActor._

  lazy val dictionaryIdMappingActor = lookup(DictionaryIdMappingActor.Id)
  lazy val putFriendsGraphActor = lookup(GraphPutFriendsActor.Id)
  lazy val updateStatisticActor = lookup(UpdateStatisticActor.Id)

  def receive = {
    case legacyFriendship: LegacyFriendship =>
      val result = for {
        newIdA <- lookupByLegacyId(legacyFriendship.legacyidA, MappingCollection.user).map(UserId)
        newIdB <- lookupByLegacyId(legacyFriendship.legacyidB, MappingCollection.user).map(UserId)
        userGroupsAB <- Future.sequence(legacyFriendship.usergroupsAB.map(exchangeLegacyUserGroup)).map(wat => wat.withFilter(!_.isEmpty).map(UserGroupType.get(_).get))
        userGroupsBA <- Future.sequence(legacyFriendship.usergroupsBA.map(exchangeLegacyUserGroup)).map(wat => wat.withFilter(!_.isEmpty).map(UserGroupType.get(_).get))
        friendship <- saveFriends(newIdA, newIdB)
        _ <- saveUserGroups(newIdA, newIdB, userGroupsAB)
        _ <- saveUserGroups(newIdB, newIdA, userGroupsBA)
      } yield {
        updateStatisticActor ! UpdateStatisticActor.ChangeFriendshipCounters(Set(newIdA, newIdB), 1)
        MigrationResponse(FriendshipMigrationResult(legacyFriendship.legacyidA, legacyFriendship.legacyidB, newIdA.id, newIdB.id, "friendship_migrator"))
      }
      result.pipeTo(sender)
  }

  def exchangeLegacyUserGroup(legacyUserGroupId: Int): Future[String] =
    dictionaryIdMappingActor.ask(DictionaryIdMappingActor.GetUserGroup(legacyUserGroupId)).mapTo[String]

  def saveFriends(userIdA: UserId, userIdB: UserId) =
    putFriendsGraphActor.ask(new GraphPutFriends(userIdA, userIdB)).mapTo[GraphFriendship]

  def saveUserGroups(userIdA: UserId, userIdB: UserId, userGroups: Seq[UserGroupType]) =
    putFriendsGraphActor.ask(new GraphPutUserGroups(userIdA, userIdB, userGroups.toFriendshipType.toArray))
}