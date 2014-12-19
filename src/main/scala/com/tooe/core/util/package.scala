package com.tooe.core
import com.tooe.core.main.SharedActorSystem._
import scala.util.Try

package object util {
  val config = defaultAkkaConfig

  object Images {

    object PaymentSystems {
      object Image {
        val prefix = "images.payment_systems"
        val default = "unknown"

        def getImage(name: String) = Try(config.getString(s"$prefix.$name"))
          .orElse(Try(config.getString(s"$prefix.$default"))).toOption.getOrElse("")

      }
      object Full {
        val Media = config.getString("images.paymentsystems.full.media")
      }
    }

    object Userphotoalbums {
      object Full {
        object Self {
          val Media = config.getString("images.userphotoalbums.full.self.media")
        }
      }
    }
    object Userssearch {
      object Full {
        object User {
          val Media = config.getString("images.userssearch.full.user.media")
        }
      }
    }
    object Star {
      object Full {
        object Self {
          val Media = config.getString("images.star.full.self.media")
        }
      }
    }
    object Locationschainstats {
      object Novelue {
        val Novalue = config.getString("images.locationschainstats.novelue.novalue")
      }
    }
    object Locationschain {
      object Full {
        object Self {
          val Media = config.getString("images.locationschain.full.self.media")
        }
      }
    }
    object Locationssearch {
      object Full {
        object Location {
          val Media = config.getString("images.locationssearch.full.location.media")
        }
        object Category {
          val Icon = config.getString("images.locationssearch.full.category.icon")
          val Media = config.getString("images.locationssearch.full.category.media")
        }
      }
    }
    object User_mainscreen {
      object Full {
        object Media {
          val Default = config.getString("images.user_mainscreen.full.media.default")
          val Main = config.getString("images.user_mainscreen.full.media.main")
          val Background = config.getString("images.user_mainscreen.full.media.background")
        }
        val Lastphotos = config.getString("images.user_profile.full.lastphotos")
      }
      object Short {
        object Media {
          val Main = config.getString("images.user_mainscreen.short.media.main")
        }
      }
      object Mini {
        object Media {
          val Main = config.getString("images.user_mainscreen.mini.media.main")
        }
      }
    }
    object Locationnews {
      object Full {
        object Like {
          val Author = config.getString("images.locationnews.full.like.author")
        }
        object Location {
          val Media = config.getString("images.locationnews.full.location.media")
        }
      }
    }
    object Wish {
      object Short {
        object Product {
          val Media = config.getString("images.wish.short.product.media")
        }
        object Location {
          val Media = config.getString("images.wish.short.location.media")
        }
      }
      object Full {
        object Like {
          val Media = config.getString("images.wish.full.like.media")
        }
        object Location {
          val Media = config.getString("images.wish.full.location.media")
        }
        object Product {
          val Media = config.getString("images.wish.full.product.media")
        }
      }
    }
    object Photoalbum {
      object Full {
        object Self {
          val Media = config.getString("images.photoalbum.full.self.media")
        }
      }
      object Short {
        object Self {
          val Media = config.getString("images.photoalbum.short.self.media")
        }
      }
    }
    object Mutualfriends {
      object Full {
        object User {
          val Media = config.getString("images.mutualfriends.full.user.media")
        }
      }
    }
    object Photoalbumphotos {
      object Full {
        object Photo {
          val Media = config.getString("images.photoalbumphotos.full.photo.media")
        }
      }
    }
    object Starscategories {
      object Full {
        object Self {
          val Media = config.getString("images.starscategories.full.self.media")
        }
      }
    }
    object Photolikes {
      object Full {
        object Author {
          val Media = config.getString("images.photolikes.full.author.media")
        }
      }
    }
    object Favoritelocations {
      object Full {
        object Category {
          val Icon = config.getString("images.favoritelocations.full.category.icon")
          val Media = config.getString("images.favoritelocations.full.category.media")
        }
        object Self {
          val Media = config.getString("images.favoritelocations.full.self.media")
        }
      }
    }
    object Locationsubscribers {
      object Full {
        object User {
          val Media = config.getString("images.locationsubscribers.full.user.media")
        }
      }
    }
    object Photocomments {
      object Full {
        object Author {
          val Media = config.getString("images.photocomments.full.author.media")
        }
      }
    }
    object Favoritesstatistics {
      object Novelue {
        val Novalue = config.getString("images.favoritesstatistics.novelue.novalue")
      }
    }
    object Lphotoalbumphotos {
      object Photos {
        val Media = config.getString("images.lphotoalbumphotos.photos.media")
      }
    }
    object Locationproductssearch {
      object Full {
        object Product {
          val Media = config.getString("images.locationproductssearch.full.product.media")
        }
      }
    }
    object Lphotoalbumssearch {
      object Full {
        object Album {
          val Media = config.getString("images.lphotoalbumssearch.full.album.media")
        }
      }
    }
    object Users_online {
      object Full {
        object User {
          val Media = config.getString("images.users_online.full.user.media")
        }
      }
    }
    object Locationphoto {
      object Short {
        object Media {
          val Self = config.getString("images.locationphoto.short.media.self")
        }
      }
      object Full {
        object Like {
          val Author = config.getString("images.locationphoto.full.like.author")
        }
        object Actor {
          val Media = config.getString("images.locationphoto.full.actor.media")
        }
        object Media {
          val Self = config.getString("images.locationphoto.full.media.self")
        }
      }
    }
    object Productssearch {
      object Full {
        object Product {
          val Media = config.getString("images.productssearch.full.product.media")
        }
      }
    }
    object Photo {
      object Short {
        object Self {
          val Media = config.getString("images.photo.short.self.media")
        }
        object Author {
          val Media = config.getString("images.photo.short.author.media")
        }
      }

      object Full {
        object Self {
          val Media = config.getString("images.photo.full.self.media")
        }
        object Author {
          val Media = config.getString("images.photo.full.author.media")
        }
        object Likes {
          val Author = config.getString("images.photo.full.likes.author")
        }
        object Comments {
          val Author = config.getString("images.photo.full.comments.author")
        }
      }
    }
    object Mypresents {
      object Full {
        object Actor {
          val Media = config.getString("images.mypresents.full.actor.media")
        }
        object Product {
          val Media = config.getString("images.mypresents.full.product.media")
        }
        object Location {
          val Media = config.getString("images.mypresents.full.location.media")
        }
      }
    }
    object MySentPresents {
      object Full {
        object Recipient {
          val Media = config.getString("images.mysentpresents.full.recipient.media")
        }
        object Product {
          val Media = config.getString("images.mysentpresents.full.product.media")
        }
      }
    }
    object PresentsAdminSearch {
      object Full {
        object Actor {
          val Media = config.getString("images.adm_presentcodesearch.full.actor.media")
        }
        object Product {
          val Media = config.getString("images.adm_presentcodesearch.full.product.media")
        }
        object Location {
          val Media = config.getString("images.adm_presentcodesearch.full.location.media")
        }
      }
    }

    object LocationPresents {
      object Full {
        object Product {
          val Media = config.getString("images.adm_locationpresents.full.product.media")
        }
      }
    }

    object News {
      object Full {
        object Wish {
          val Media = config.getString("images.news.full.wish.media")
        }
        object Photoalbum {
          val Photos = config.getString("images.news.full.photoalbum.photos")
        }
        object Recepient {
          val Media = config.getString("images.news.full.recepient.media")
        }
        object Photo {
          val Main = config.getString("images.news.full.photo.main")
        }
        object Likes {
          val Media = config.getString("images.news.full.likes.media")
        }
        object Location {
          val Media = config.getString("images.news.full.location.media")
        }
        object Present {
          val Media = config.getString("images.news.full.present.media")
        }
        object Comments {
          val Media = config.getString("images.news.full.comments.media")
        }
        object Actor {
          val Media = config.getString("images.news.full.actor.media")
        }
      }
    }
    object Lforcheckinsearch {
      object Full {
        object Location {
          val Media = config.getString("images.lforcheckinsearch.full.location.media")
        }
        object Category {
          val Icon = config.getString("images.lforcheckinsearch.full.category.icon")
          val Media = config.getString("images.lforcheckinsearch.full.category.media")
        }
      }
    }
    object Friendshiprequests {
      object Full {
        object User {
          val Media = config.getString("images.friendshiprequests.full.user.media")
        }
      }
    }
    object Location {
      object Mini {
        object Self {
          val Lastphoto = config.getString("images.location.mini.self.lastphoto")
          val Main = config.getString("images.location.mini.self.main")
        }
      }
      object Short {
        object Self {
          val Media = config.getString("images.location.short.self.media")
          val Lastphotos = config.getString("images.location.short.self.lastphotos")
        }
        object Users {
          val Media = config.getString("images.location.short.users.media")
        }
        object Friends {
          val Media = config.getString("images.location.short.friends.media")
        }
      }
      object Adm {
        object Self {
          val Media = config.getString("images.location.adm.self.media")
        }
      }
      object Full {
        object Self {
          val Lastphotos = config.getString("images.location.full.self.lastphotos")
          val Media = config.getString("images.location.full.self.media")
        }
      }
    }

    object PreModerationLocation {
      object Full {
        object Self{
          val Media = config.getString("images.adm_locationmod.full.self.media")
        }
      }
    }
    object PreModerationLocationSearch {
      object Full {
        object Self{
          val Media = config.getString("images.adm_locationmodadmsearch.full.self.media")
        }
      }
    }
    object ModerationLocationSearch {
      object Full {
        object Self{
          val Media = config.getString("images.adm_locationmodsearch.full.self.media")
        }
      }
    }

    object Product {
      object Full {
        object Location {
          val Media = config.getString("images.product.full.location.media")
        }
        object Self {
          val Media = config.getString("images.product.full.self.media")
        }
        object Loup {
          val Media = config.getString("images.product.full.loup.media")
        }
      }
      object Admin {
        object Self {
          val Media = config.getString("images.product.admin.self.media")
        }
      }
    }
    object CompanyProducts {
      object Full {
        object Self {
          val Media = config.getString("images.adm_companyproducts.full.self.media")
        }
      }
    }
    object Checkin {
      object Full {
        object Location {
          val Media = config.getString("images.checkin.full.location.media")
        }
        object User {
          val Media = config.getString("images.checkin.full.user.media")
        }
      }
    }
    object Wishlikes {
      object Full {
        object Author {
          val Media = config.getString("images.wishlikes.full.author.media")
        }
      }
    }
    object Locationphotolikes {
      object Full {
        object Author {
          val Media = config.getString("images.locationphotolikes.full.author.media")
        }
      }
    }
    object Locationcheckins {
      object Short {
        object Users {
          val Media = config.getString("images.locationcheckins.short.users.media")
        }
        object Friends {
          val Media = config.getString("images.locationcheckins.short.friends.media")
        }
      }
    }
    object Checkinssearch {
      object User {
        val Media = config.getString("images.checkinssearch.user.media")
      }
    }
    object Wishes {
      object Short {
        object Product {
          val Media = config.getString("images.wishes.short.product.media")
        }
        object Location {
          val Media = config.getString("images.wishes.short.location.media")
        }
      }
      object Full {
        object Like {
          val Media = config.getString("images.wishes.full.like.media")
        }
        object Location {
          val Media = config.getString("images.wishes.full.location.media")
        }
        object Product {
          val Media = config.getString("images.wishes.full.product.media")
        }
      }
      object Mini {
        object Location {
          val Media = config.getString("images.wishes.mini.location.media")
        }
        object Product {
          val Media = config.getString("images.wishes.mini.product.media")
        }
      }
    }
    object Lphotoalbum {
      object Short {
        object Self {
          val Media = config.getString("images.lphotoalbum.short.self.media")
        }
      }
      object Full {
        object Self {
          val Media = config.getString("images.lphotoalbum.full.self.media")
        }
      }
    }
    object Newscomments {
      object Full {
        object Author {
          val Media = config.getString("images.newscomments.full.author.media")
        }
      }
    }
    object Userfriendssearch {
      object Full {
        object User {
          val Media = config.getString("images.userfriendssearch.full.user.media")
        }
      }
    }
    object Friendssearch {
      object Full {
        object User {
          val Media = config.getString("images.friendssearch.full.user.media")
        }
      }
    }
    object User {
      object Mini {
        object Self {
          val Media = config.getString("images.user.mini.self.media")
        }
      }
      object Short {
        object Self {
          val Media = config.getString("images.user.short.self.media")
        }
        object Lastphotos {
          val Media = config.getString("images.user.short.lastphotos.media")
        }
      }
      object Full {
        object Self {
          val Media = config.getString("images.user.full.self.media")
          val Default = config.getString("images.user.full.self.default")
          val Background = config.getString("images.user.full.self.background")
        }
        object Lastphotos {
          val Media = config.getString("images.user.full.lastphotos.media")
        }
      }
    }
    object Lchainsearch {
      object Full {
        object Location {
          val Media = config.getString("images.lchainsearch.full.location.media")
        }
        object Category {
          val Media = config.getString("images.lchainsearch.full.category.media")
          val Icon = config.getString("images.lchainsearch.full.category.icon")
        }
      }
    }
    object Lregionsearch {
      object Full {
        object Locations {
          val Media = config.getString("images.lregionsearch.full.locations.media")
        }
      }
    }
    object Userevents {
      object Full {
        object Actor {
          val Media = config.getString("images.userevents.full.actor.media")
        }
        object Location {
          val Media = config.getString("images.userevents.full.location.media")
        }
        object Present {
          val Media = config.getString("images.userevents.full.present.media")
        }
        object Photo {
          val Media = config.getString("images.userevents.full.photo.media")
        }
      }
    }
    object Friends {
      object Full {
        object User {
          val Media = config.getString("images.friends.full.user.media")
        }
      }
    }
    object Newslikes {
      object Full {
        object Author {
          val Media = config.getString("images.newslikes.full.author.media")
        }
      }
    }
    object User_profile {
      object Short {
        object Media {
          val Main = config.getString("images.user_profile.short.media.main")
        }
      }
      object Mini {
        object Media {
          val Main = config.getString("images.user_profile.mini.media.main")
        }
      }
      object Full {
        val Lastphotos = config.getString("images.user_profile.full.lastphotos")
        object Media {
          val Background = config.getString("images.user_profile.full.media.background")
          val Default = config.getString("images.user_profile.full.media.default")
          val Main = config.getString("images.user_profile.full.media.main")
        }
      }
    }
    object Mylocationsubscriptions {
      object Full {
        object Location {
          val Media = config.getString("images.mylocationsubscriptions.full.location.media")
        }
      }
    }
    object Productcategories {
      object Full {
        object Self {
          val Media = config.getString("images.productcategories.full.self.media")
          val Icon = config.getString("images.productcategories.full.self.icon")
        }
      }
    }
    object CompanyAdmSearch {
      object Full {
        object Self {
          val Media = config.getString("images.adm_companysearch.full.self.media")
        }
      }
    }
    object CompanyModerationAdmSearch {
      object Full {
        object Self {
          val Media = config.getString("images.adm_companymodsearch.full.self.media")
        }
      }
    }
    object CompanyModeration {
      object Full {
        object Self {
          val Media = config.getString("images.adm_companymod.full.self.media")
        }
      }
    }
    object Company {
      object Full {
        object Self {
          val Media = config.getString("images.company.full.self.media")
        }
        object Media {
          val Media = config.getString("images.company.full.media.media")
        }
      }
    }
    object AdminCompany {
      object Full {
        object Self {
          val Media = config.getString("images.adm_company.full.self.media")
        }
      }
    }
  }
}