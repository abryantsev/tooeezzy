package com.tooe.core.db.graph.test.data

import com.tooe.core.domain.{LocationId, UserGroupType, UserId}
import org.bson.types.ObjectId
import com.tooe.core.db.graph.domain.FriendshipType
import scala.collection.JavaConverters._

trait TestDataFriends {

  object TestKeysConverter {
    import scala.collection.JavaConverters._
    implicit def stringToUserId(id: String) = UserId(new ObjectId(id))
    implicit def stringToLocationId(id: String) = LocationId(new ObjectId(id))
    implicit def listStringToCollLocationId(list: java.util.List[String]) = list.asScala.map(l => LocationId(new ObjectId(l))).asJavaCollection
    implicit def listStringToCollUserId(list: java.util.List[String]) = list.asScala.map(l => UserId(new ObjectId(l))).asJavaCollection
    implicit def stringToUserGroupType(groupType: String) = UserGroupType.get(groupType).getOrElse(UserGroupType.Friend)
  }

  val userId1, userId2, userId3, userId22  = new ObjectId().toString

  def notExistingUser = UserId(new ObjectId())
  def notExistingLocation = LocationId(new ObjectId())

  val friends12 = java.util.Arrays.asList(userId1, userId2)
  val friends13 = java.util.Arrays.asList(userId1, userId3)
  val emptyFriends = new java.util.ArrayList[String]()

  val emptyGroup = FriendshipType.FRIEND

  val friendslist1 = java.util.Arrays.asList(userId2, userId3)
  val friendslist1a = java.util.Arrays.asList(userId3)
  val friendslist2 = java.util.Arrays.asList(userId1)
  val friendslist3 = friendslist2
  val mutualFriendslist = java.util.Arrays.asList(userId1)

  val usergroupsEmpty = Array[FriendshipType]()
  val usergroups12 = Array[FriendshipType](FriendshipType.MYFRIEND, FriendshipType.FAMILY)
  val usergroups12asList = usergroups12.toList.asJava
  val usergroups13 = Array[FriendshipType](FriendshipType.MYFRIEND, FriendshipType.COLLEAGUE)
  val usergroups13asList = usergroups13.toList.asJava
  val usergroupsEmptyasList = usergroupsEmpty.toList.asJava
  val usergroup1 = FriendshipType.FAMILY
  val usergroup2 = FriendshipType.MYFRIEND
  val usergroup1friendslist = java.util.Arrays.asList(userId2)
  val usergroup2friendslist = friendslist1
}
