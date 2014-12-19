package com.tooe.core.domain

import com.tooe.core.db.mongo.util.{HasIdentityFactoryEx, UnmarshallerEntity, HasIdentity}


sealed trait UsersOwnStatisticsViewType extends HasIdentity with UnmarshallerEntity{
  def id: String
}

object UsersOwnStatisticsViewType extends HasIdentityFactoryEx[UsersOwnStatisticsViewType] {

  object Presents extends UsersOwnStatisticsViewType {
    def id = "presents"
  }
  object PresentsType extends UsersOwnStatisticsViewType {
    def id = "presentstype"
  }
  object CertificatesType extends UsersOwnStatisticsViewType {
      def id = "certificatestype"
  }
  object SentPresents extends UsersOwnStatisticsViewType {
      def id = "sentpresents"
  }
  object SentPresentsType extends UsersOwnStatisticsViewType {
      def id = "sentpresentstype"
  }
  object SentCertificatesType extends UsersOwnStatisticsViewType {
      def id = "sentcertificatestype"
  }
  object Friends extends UsersOwnStatisticsViewType {
    def id = "friends"
  }
  object FriendsRequests extends UsersOwnStatisticsViewType {
    def id = "friendsreqs"
  }
  object FriendsOnline extends UsersOwnStatisticsViewType {
    def id = "friendsonline"
  }
  object Wishes extends UsersOwnStatisticsViewType {
    def id = "wishes"
  }
  object FulfilledWishes extends UsersOwnStatisticsViewType {
    def id = "ffwishes"
  }
  object Subscriptions extends UsersOwnStatisticsViewType {
    def id = "subs"
  }
  object StarsSubscriptions extends UsersOwnStatisticsViewType {
    def id = "starssubs"
  }
  object LocationsSubscriptions extends UsersOwnStatisticsViewType {
    def id = "locationssubs"
  }
  object PhotoAlbums extends UsersOwnStatisticsViewType {
    def id = "photoalbums"
  }
  object NewEvents extends UsersOwnStatisticsViewType {
    def id = "newevents"
  }
  object NewPresents extends UsersOwnStatisticsViewType {
    def id = "newpresents"
  }
  object Favorites extends UsersOwnStatisticsViewType {
    def id = "favorites"
  }
  object Full extends UsersOwnStatisticsViewType {
    def id = "none"
  }

  val values = Seq(Presents, PresentsType, CertificatesType, SentPresents, SentPresentsType, SentCertificatesType, Friends, FriendsRequests, FriendsOnline, Wishes, FulfilledWishes, Subscriptions, StarsSubscriptions, LocationsSubscriptions, PhotoAlbums, NewEvents, NewPresents, Favorites)
  val valuesWithoutFriendsOnline = values.filterNot(_ == UsersOwnStatisticsViewType.FriendsOnline)
}

