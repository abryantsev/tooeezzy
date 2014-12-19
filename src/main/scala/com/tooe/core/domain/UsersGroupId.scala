package com.tooe.core.domain

import com.tooe.core.db.graph.domain.FriendshipType


case class UsersGroupId(id: String)

object UserGroupId {
  val Me = UsersGroupId("me")
}

object UsersGroupId{
  implicit def friendshipTypeHelper(userGroups: Seq[UsersGroupId]) = new {
      def toFriendshipType = userGroups.map(ug => FriendshipType.valueOf(ug.id.toUpperCase))
  }
}