package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactory, UnmarshallerEntity, HasIdentity}
import com.tooe.core.db.graph.domain.FriendshipType

sealed trait UserGroupType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object UserGroupType extends HasIdentityFactory[UserGroupType] {

  object Family extends UserGroupType {
    def id = "family"
  }

  object MyFriend extends UserGroupType {
    def id = "myfriend"
  }

  object BestFriend extends UserGroupType {
    def id = "bestfriend"
  }

  object Colleague extends UserGroupType {
    def id = "colleague"
  }

  object Friend extends UserGroupType {
    def id = "friend"
  }

  val values = Seq(Family, MyFriend, BestFriend, Colleague)
  private val idToVal = values.map(v => v.id -> v).toMap

  def get(id: String) = idToVal.get(id)

  implicit def friendshipTypeHelper(userGroup: UserGroupType) = new {
    def toFriendshipType = FriendshipType.valueOf(userGroup.id.toUpperCase)
  }
  implicit def friendshipTypeSeqHelper(userGroups: Seq[UserGroupType]) = new {
    def toFriendshipType = userGroups.map(ug => FriendshipType.valueOf(ug.id.toUpperCase))
  }
}

