package com.tooe.core.db.graph.test.data

trait TestDataGetFriends {

  val userId = "51197909444fddfecf2a6690"
    
  val groupFamily = "family"
  val groupFriends = "friends"
  val groupBestFriends = "bestfriends"
  val emptyGroup = null
//  val groups = Array("friends", "family")
//  val emptyGroups = Array.empty[String]
    
  val friends = java.util.Arrays.asList("51197909444fddfecf2a6691", "51197909444fddfecf2a668f")
  val friendsGroupFamily = java.util.Arrays.asList("51197909444fddfecf2a6691", "51197909444fddfecf2a668f")
  val friendsGroupFriends = java.util.Arrays.asList("51197909444fddfecf2a6691")
  val friendsGroupBestFriends = new java.util.ArrayList[String]()
}
