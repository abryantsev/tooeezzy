package com.tooe.core.migration.api

import akka.actor.ActorSystem
import akka.pattern._
import com.tooe.api.service.SprayServiceBaseClass2
import com.tooe.core.migration._
import spray.http.StatusCodes

class MigrationService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  lazy val companyMigratorActor = lookup(CompanyMigratorActor.Id)
  lazy val userMigratorActor = lookup(UserMigratorActor.Id)
  lazy val adminUserMigratorActor = lookup(AdminUserMigratorActor.Id)
  lazy val photoMigratorActor = lookup(PhotoMigratorActor.Id)
  lazy val locationMigratorActor = lookup(LocationMigratorActor.Id)
//  lazy val starSubscriptionMigratorActor = lookup(StarSubscriptionMigrator.Id)
  lazy val productMigratorActor = lookup(ProductMigratorActor.Id)
  lazy val locationPhotoMigratorActor = lookup(LocationPhotoMigratorActor.Id)
  lazy val wishMigratorActor = lookup(WishMigratorActor.Id)
  lazy val presentMigratorActor = lookup(PresentMigratorActor.Id)
//  lazy val promotionMigratorActor = lookup(PromotionMigratorActor.Id)
  lazy val favoriteMigratorActor = lookup(FavoriteMigratorActor.Id)
  lazy val friendshipRequestMigratorActor = lookup(FriendshipRequestMigratorActor.Id)
  lazy val locationNewsMigratorActor = lookup(LocationNewsMigratorActor.Id)
  lazy val locationSubscriptionActor = lookup(LocationSubscriptionMigratorActor.Id)
  lazy val friendshipMigratorActor = lookup(FriendshipMigratorActor.Id)
  lazy val locationsChainMigratorActor = lookup(LocationChainMigratorActor.Id)
  lazy val newsMigratorActor = lookup(NewsMigratorActor.Id)
  lazy val userEventMigratorActor = lookup(UserEventMigratorActor.Id)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val route = pathPrefix("migration") { post {
    path("company_migrator") {
      entity(as[CompanyMigratorActor.LegacyCompany]) { legacyCompanyRequest: CompanyMigratorActor.LegacyCompany =>
        complete(StatusCodes.Created, (companyMigratorActor ? legacyCompanyRequest).mapTo[MigrationResponse])
      }
    } ~
    path("user_migrator") {
      entity(as[UserMigratorActor.LegacyUser]) {  legacyUserRequest: UserMigratorActor.LegacyUser =>
        complete(StatusCodes.Created, (userMigratorActor ? legacyUserRequest).mapTo[MigrationResponse])
      }
    } ~
    path("adm_user_migrator") {
      entity(as[AdminUserMigratorActor.LegacyAdminUser]) {  legacyAdminRequest: AdminUserMigratorActor.LegacyAdminUser =>
        complete(StatusCodes.Created, (adminUserMigratorActor ? legacyAdminRequest).mapTo[MigrationResponse])
      }
    } ~
    path("photo_migrator") {
      entity(as[PhotoMigratorActor.LegacyPhotoAlbum]) {  legacyPhotoRequest: PhotoMigratorActor.LegacyPhotoAlbum =>
        complete(StatusCodes.Created, (photoMigratorActor ? legacyPhotoRequest).mapTo[MigrationResponse])
      }
    } ~
    path("location_migrator") {
      entity(as[LocationMigratorActor.LegacyLocation]) {  legacyLocationRequest: LocationMigratorActor.LegacyLocation =>
        complete(StatusCodes.Created, (locationMigratorActor ? legacyLocationRequest).mapTo[MigrationResponse])
      }
    } ~/*
    path("starsubscription_migrator") {
      entity(as[StarSubscriptionMigrator.LegacyStarSubscribe]) {  legacySubscription: StarSubscriptionMigrator.LegacyStarSubscribe =>
        complete(StatusCodes.Created, (starSubscriptionMigratorActor ? legacySubscription).mapTo[MigrationResponse])
      }
    } ~*/
    path("product_migrator") {
      entity(as[ProductMigratorActor.LegacyProduct]) {  legacyProduct: ProductMigratorActor.LegacyProduct =>
        complete(StatusCodes.Created, (productMigratorActor ? legacyProduct).mapTo[MigrationResponse])
      }
    } ~
    path("locationphoto_migrator") {
      entity(as[LocationPhotoMigratorActor.LegacyLocationPhotoAlbum]) { legacyAlbum: LocationPhotoMigratorActor.LegacyLocationPhotoAlbum =>
        complete(StatusCodes.Created, (locationPhotoMigratorActor ? legacyAlbum).mapTo[MigrationResponse])}
    } ~
    path("wish_migrator") {
      entity(as[WishMigratorActor.LegacyWish]) {legacyWish: WishMigratorActor.LegacyWish =>
        complete(StatusCodes.Created, (wishMigratorActor ? legacyWish).mapTo[MigrationResponse])}
    } ~
    path("present_migrator") {
      entity(as[PresentMigratorActor.LegacyPresent]) {legacyPresent: PresentMigratorActor.LegacyPresent =>
        complete(StatusCodes.Created, (presentMigratorActor ? legacyPresent).mapTo[MigrationResponse])}
    } ~/*
    path("promotion_migrator") {
      entity(as[PromotionMigratorActor.LegacyPromotion]) {legacyPromo: PromotionMigratorActor.LegacyPromotion =>
        complete(StatusCodes.Created, (promotionMigratorActor ? legacyPromo).mapTo[MigrationResponse])}
    } ~*/
    path("favorite_migrator") {
      entity(as[FavoriteMigratorActor.LegacyFavorite]) {legacyFavorite: FavoriteMigratorActor.LegacyFavorite =>
        complete(StatusCodes.Created, (favoriteMigratorActor ? legacyFavorite).mapTo[MigrationResponse])}
    } ~
    path("locationnews_migrator") {
      entity(as[LocationNewsMigratorActor.LegacyLocationNews]) { legacyLocationNews: LocationNewsMigratorActor.LegacyLocationNews =>
        complete(StatusCodes.Created, (locationNewsMigratorActor ? legacyLocationNews).mapTo[MigrationResponse])}
    } ~
    path("friendshiprequest_migrator") {
      entity(as[FriendshipRequestMigratorActor.LegacyFriendshipRequests]) {friendship: FriendshipRequestMigratorActor.LegacyFriendshipRequests =>
        complete(StatusCodes.Created, (friendshipRequestMigratorActor ? friendship).mapTo[MigrationResponse])}
    } ~
    path("locationsubscription_migrator") {
      entity(as[LocationSubscriptionMigratorActor.LegacyLocationSubscription]) {subscription: LocationSubscriptionMigratorActor.LegacyLocationSubscription =>
        complete(StatusCodes.Created, (locationSubscriptionActor ? subscription).mapTo[MigrationResponse])}
    } ~
    path("friendship_migrator") {
      entity(as[FriendshipMigratorActor.LegacyFriendship]) {friendship: FriendshipMigratorActor.LegacyFriendship =>
        complete(StatusCodes.Created, (friendshipMigratorActor ? friendship).mapTo[MigrationResponse])}
    } ~
    path("locationschain_migrator") {
      entity(as[LocationChainMigratorActor.LegacyLocationChain]) {chain: LocationChainMigratorActor.LegacyLocationChain =>
        complete(StatusCodes.Created, (locationsChainMigratorActor ? chain).mapTo[MigrationResponse])}
    } ~
    path("news_migrator") {
      entity(as[NewsMigratorActor.LegacyNews]) {news: NewsMigratorActor.LegacyNews =>
        complete(StatusCodes.Created, (newsMigratorActor ? news).mapTo[MigrationResponse])}
    } ~
    path("userevent_migrator") {
      entity(as[UserEventMigratorActor.LUserEvent]) {event: UserEventMigratorActor.LUserEvent =>
        complete(StatusCodes.Created, (userEventMigratorActor ? event).mapTo[MigrationResponse])}
    }
  }}
}
