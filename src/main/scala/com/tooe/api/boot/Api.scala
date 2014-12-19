package com.tooe.api.boot

import akka.actor.Props
import spray.routing._
import com.tooe.core.boot.Core
import com.tooe.api.service._
import com.tooe.core.application.AppActors
import com.tooe.core.usecase.{WishWriteActor, InfoMessageActor, WishReadActor}
import com.tooe.core.usecase.payment.PlatronStrategyActor
import com.tooe.extensions.scala.Settings
import com.tooe.core.main.SharedActorSystem
import com.tooe.core.migration.api.MigrationService
import com.tooe.util.correction.CorrectionService

trait Api extends RouteConcatenation with AppActors {
  this: Core =>

  println("tooe >> ... init Api ...")

  val apiRoutes =
    new BasicUserService().route ~
    new SessionService().route ~
    new CheckinService().route ~
    new UserService().route ~
    new SecurityService().route ~
    new LocationService().route ~
    new ProductService().route ~
    new CountryService().route ~
    new RegionService().route ~
    new FriendService().route ~
    new FriendshipRequestService().route ~
    new PresentService().route ~
    wishesService.route ~
    new PaymentService().route ~
    new PaymentSystemService().route ~
    new PromotionService().route ~
    new PromotionVisitorService().route ~
    new PhotoAlbumService().route ~
    new PhotoService().route ~
    new LocationPhotoAlbumService().route ~
    new LocationPhotoService().route ~
    new UserEventService().route ~
    new LocationSubscriptionService().route ~
    new LocationNewsService().route ~
    new UserProfilesService().route ~
    new StarsCategoriesService().route ~
    new StarSubscriptionService().route ~
    new StarService().route ~
    new AdminSessionService().route ~
    new CompaniesService().route ~
    new AdminUserService().route ~
    new CompaniesExportService().route ~
    new AdminUserEventService().route ~
    new UsersGroupService().route ~
    new CurrencyService().route ~
    new MaritalStatusService().route ~
    new PeriodService().route ~
    new ModerationStatusService().route ~
    new CompaniesModerationService().route~
    new UserCommentService().route ~
    new OnlineStatusService().route ~
    new AdminRoleService().route ~
    new LifecycleStatusService().route ~
    new MaritalStatusService().route ~
    new LocationsChainsService().route ~
    new EventGroupService().route ~
    new NewsCommentService().route ~
    new LocationModerationService().route ~
    new PreModerationLocationService().route ~
    new NewsService().route ~
    new NewsCommentService().route ~
    new CalendarEventService().route ~
    new OrderExportService().route

  var routes = new HomeService().route
  
  if(settings.Api.coreRoutesEnable) {
  	println("tooe >> ... init core routes ...")
    routes = routes ~ apiRoutes
  }
  if(settings.Api.testCoreRoutesEnable) {
  	println("tooe >> ... init test core routes ...")
 	  // Note: place there any non production service
	  // may be before or after api's routes
	  // testRouteBefore ~ apiRoutes ~ testRouteAfter
   routes = routes ~ 
    new CreateLinkMediaServerService().route ~
    new TestService1().route    
  }
  if(settings.Api.testGraphRoutesEnable) {
  	println("tooe >> ... init test graph routes ...")
 	  // Note: place there any non production service
	  // may be before or after api's routes
	  // testRouteBefore ~ apiRoutes ~ testRouteAfter
   routes = routes ~
  	new TestServiceGraph().route
  }
  if(settings.Api.migrationRoutesEnable) {
    println("tooe >> ... init migration routes ...")
    routes = routes ~
      new MigrationService().route
  }
  if(settings.Api.correctionRoutesEnable) {
    println("tooe >> ... init correction routes ...")
    routes = routes ~ new CorrectionService().route

  }
  
  val rootService = actorSystem.actorOf(Props(new RoutedHttpService(routes)), "spray")

  def wishesService = new WishService(
    wishReadActor = lookup(WishReadActor.Id) ,
    wishWriteActor = lookup(WishWriteActor.Id)
  )
}

trait DefaultTimeout {

  implicit val timeout = Settings(SharedActorSystem.sharedMainActorSystem).DEFAULT_ACTOR_TIMEOUT
}

//Note: empty trait for easy indicate test routes.
trait TestService
