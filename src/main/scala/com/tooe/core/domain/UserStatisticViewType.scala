package com.tooe.core.domain
import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}


sealed trait UserStatisticViewType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object UserStatisticViewType extends HasIdentityFactoryEx[UserStatisticViewType] {

  object Friends extends UserStatisticViewType {
    def id = "friends"
  }

  object FriendsOnline extends UserStatisticViewType {
    def id = "friendsonline"
  }

  object Wishes extends UserStatisticViewType {
    def id = "wishes"
  }

  object PhotoAlbums extends UserStatisticViewType {
    def id = "photoalbums"
  }

  object Favorites extends UserStatisticViewType {
    def id = "favorites"
  }

  object Full extends UserStatisticViewType {
    def id = "none"
  }

  val FriendsOnlineBlock = Set(FriendsOnline)
  val FullBlock = Set(Full)

  val values = Seq(Friends, FriendsOnline, Wishes, PhotoAlbums, Favorites)
}


