package com.tooe.core.application

import scala.concurrent.duration.DurationInt
import org.springframework.data.mongodb.core.MongoTemplate
import com.tooe.core.db.graph.msg.{ TitanLoad, GraphFriends, GraphGetFriends }
import com.tooe.core.db.mongo.MongoDataActor
import com.tooe.core.db.graph._
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.extensions.scala.Settings
import akka.actor._
import akka.actor.SupervisorStrategy.Restart
import akka.event.Logging
import com.tooe.core.usecase._
import checkin.CheckinDataActor
import domain.FriendshipType
import friends_cache.FriendCacheDataActor
import com.tooe.core.usecase.location_category._
import com.tooe.core.usecase.location_photo._
import com.tooe.core.usecase.location_photoalbum._
import com.tooe.core.usecase.promotion._
import product.ProductDataActor
import com.tooe.core.usecase.payment._
import com.tooe.core.usecase.location._
import user.UserDataActor
import com.tooe.core.usecase.wish._
import akka.routing.FromConfig
import com.tooe.core.usecase.session._
import com.tooe.core.usecase.security.CredentialsDataActor
import com.tooe.core.usecase.present.{WelcomePresentWriteActor, FreePresentWriteActor, PresentDataActor}
import org.bson.types.ObjectId
import com.tooe.core.usecase.photo_like.PhotoLikeDataActor
import com.tooe.core.usecase.user_event.UserEventDataActor
import com.tooe.core.usecase.location_photo_like.LocationPhotoLikeDataActor
import com.tooe.core.usecase.location_photo_comment.LocationPhotoCommentDataActor
import com.tooe.core.usecase.maritalstatus.MaritalStatusDataActor
import com.tooe.core.usecase.location_subscription.LocationSubscriptionDataActor
import com.tooe.core.usecase.location_news._
import com.tooe.core.usecase.star_category.StarsCategoriesDataActor
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor
import com.tooe.core.usecase.country.CountryDataActor
import com.tooe.core.usecase.region.RegionDataActor
import com.tooe.core.usecase.company._
import com.tooe.core.migration._
import com.tooe.core.usecase.admin_user._
import akka.actor.OneForOneStrategy
import com.tooe.core.db.mongo.SaveBasic
import com.tooe.core.boot.Started
import com.tooe.core.boot.Stop
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.GetBasic
import com.tooe.core.boot.Start
import com.tooe.core.usecase.cache_writesniffer.CacheWriteSnifferDataActor
import com.tooe.core.usecase.friendshiprequest.FriendshipRequestDataActor
import com.tooe.core.usecase.online_status.OnlineStatusDataActor
import com.tooe.core.usecase.admin_user_event._
import com.tooe.core.usecase.users_group.UsersGroupDataActor
import com.tooe.core.usecase.currency.CurrencyDataActor
import com.tooe.core.usecase.period.PeriodDataActor
import com.tooe.core.usecase.moderation_status.ModerationStatusDataActor
import com.tooe.core.usecase.locationschain._
import com.tooe.core.usecase.admin_role.AdminRoleDataActor
import com.tooe.core.usecase.lifecycle_status.LifecycleStatusDataActor
import com.tooe.core.usecase.event_group.EventGroupDataActor
import com.tooe.core.usecase.favorite_stats.FavoriteStatsDataActor
import com.tooe.core.usecase.news.NewsReadActor
import com.tooe.core.usecase.urls.{UrlsWriteActor, UrlsDataActor}
import com.tooe.core.usecase.event_type.EventTypeDataActor
import com.tooe.core.usecase.job.urls_check._
import com.tooe.core.usecase.calendarevent.{CalendarEventReadActor, CalendarEventWriteActor, CalendarEventDataActor}
import com.tooe.core.usecase.payment.alfabank.AlfabankStrategyActor
import com.tooe.core.usecase.calendarevent.{CalendarEventWriteActor, CalendarEventDataActor}
import com.tooe.core.usecase.job.{ExpiredPresentsMarkerActor, OverduePaymentCancellationActor}
import com.tooe.core.usecase.payment.tooeezzy.TooeezzyStrategyActor
import com.tooe.core.usecase.mediaserver.UploadMediaServerMockActor
import com.tooe.util.correction.{LocationPhotoAndAlbumCorrector, LocationImageReplacer}

case class GetImplementation()

case class Implementation(title: String, version: String, build: String)

case class TestSettings()

case class TestInfrastructure()

case class TestData()

case class TestTitanData()

case class TestMysqlData()

case class Tested(info: String)

case class TitanLoadData()

case class PoisonPill()

class AppActorFactory {

}

class ApplicationActor(actorFactory: AppActorFactory) extends Actor with AppActors {

  val settings = Settings(context.system)

  import settings._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 1 minute) {
      case _ => Restart
      //		case _: ArithmeticException => Resume
      //		case _: NullPointerException => Restart
      //		case _: IllegalArgumentException => Stop
      //		case _: Exception => Escalate
    }

  val log = Logging(context.system, this)

  def receive = {
    case GetImplementation() =>
      val manifestStream = getClass.getResourceAsStream("/META-INF/MANIFEST.MF")
      val manifest = new java.util.jar.Manifest(manifestStream)
      val title = manifest.getMainAttributes.getValue("Implementation-Title")
      val version = manifest.getMainAttributes.getValue("Implementation-Version")
      val build = manifest.getMainAttributes.getValue("Implementation-Build")
      manifestStream.close()

      sender ! Implementation(title, version, build)

    case Start() =>
      settings.tooeProfile match {
        case "tooe-graph" => 
          startGraphProfile()
          startGraphLoader()
        case "tooe-core" =>
          startCoreProfile()
          startCoreGraphProfile()
        case "tooe-full" =>
          startCoreProfile()
          startCoreGraphProfile()
          startGraphLoader()
        case "tooe-migration" =>
          startCoreProfile()
          startCoreGraphProfile()
          startMigrationProfile()
          startGraphLoader()
        case "tooe-migration-core" =>
          startCoreProfile()
          startCoreGraphProfile()
          startMigrationProfile()
        case _ => startCoreProfile()
      }

      if (Api.correctionRoutesEnable) {
        context.actorOf(Props[LocationImageReplacer], LocationImageReplacer.Id.name)
        context.actorOf(Props[LocationPhotoAndAlbumCorrector], LocationPhotoAndAlbumCorrector.Id.name)
      }

      sender ! Started()

    /*
     * Stops this actor and all the child actors.
     */
    case Stop() =>
      context.children.foreach(context.stop)

    case PoisonPill() =>
      sys.exit(-1)

    case TestSettings() =>
      log.info("test settings ......................................... ")
      val settings = Settings(context.system)

    case TestInfrastructure() =>
      log.info("test spring/infrastructure ......................................... ")
      val mongoTemplateLookup: MongoTemplate = BeanLookup[MongoTemplate]
      log.info("mongoTemplateLookup : " + mongoTemplateLookup)

    case TestData() =>
      log.info("test data/mongo ......................................... ")
      //val mongoActor = context.actorFor("mongo")
      //val content: String = "start mongo check ... "
      //mongoActor ! new MySuperMessageImpl(content)

      1 to 10 foreach {
        i =>
          val mongoDataActor = context.actorFor("mongodata")
          mongoDataActor ! new GetBasic()
          mongoDataActor ! new SaveBasic()
          mongoDataActor ! new GetBasic()
      }

    case TitanLoadData() =>
      import akka.util.Timeout
      import akka.pattern.ask
      import scala.concurrent.ExecutionContext.Implicits.global

      Thread.sleep(5000)

      implicit val timeout = Timeout(100000)

      val titanLoadActor = context.actorFor("titanload")
      val graphGetFriends = context.actorFor("graphread")

      val user_iam = "51197909444fddfecf2a668f"
      val user_he = "51197909444fddfecf2a6690"
      val user_she = "51197909444fddfecf2a6691"

      val objectid = user_he

      val f = for {
        a <- {
          log.info("titan load/titan+cassandra ......................................... ")
          val content: String = "start titan load ... "
          titanLoadActor ? new TitanLoad()
        }
        b <- {
          log.info("titan GraphGetFriends test ......................................... ")
          val content = "reading graph content ... "
          (graphGetFriends ? new GraphGetFriends(UserId(new ObjectId(objectid)), FriendshipType.FAMILY)).mapTo[GraphFriends]
        }
      } yield b

      f foreach {
        (friends) => log.info("\n\tFriends of " + objectid + " -> " + friends.toString())
      }

    case Tested(info) =>
      log.info("Checked: " + info)
  }
  
  private def startGraphProfile() {
    println("tooe >> starting graph profile...")

    context.actorOf(Props[UserGraphActor].withRouter(FromConfig()), UserGraphActor.Id.name  + "Router")
    context.actorOf(Props[GraphGetFriendsActor].withRouter(FromConfig()), GraphGetFriendsActor.Id.name  + "Router")
    context.actorOf(Props[GraphPutFriendsActor].withRouter(FromConfig()), GraphPutFriendsActor.Id.name  + "Router")
    context.actorOf(Props[LocationGraphActor].withRouter(FromConfig()), LocationGraphActor.Id.name  + "Router")
    context.actorOf(Props[GraphGetFavoritesActor].withRouter(FromConfig()), GraphGetFavoritesActor.Id.name  + "Router")
    context.actorOf(Props[GraphPutFavoritesActor].withRouter(FromConfig()), GraphPutFavoritesActor.Id.name  + "Router")

  }

  private def startGraphLoader() {
    println("tooe >> starting graph loader...")

    context.actorOf(Props(new TitanLoadActor()), TitanLoadActor.Id.name)
  }

  private def startCoreGraphProfile() {
    println("tooe >> starting core/clustered graph profile...")

    context.actorOf(Props[UserGraphActor].withRouter(FromConfig()), UserGraphActor.Id.name)
    context.actorOf(Props[GraphGetFriendsActor].withRouter(FromConfig()), GraphGetFriendsActor.Id.name)
    context.actorOf(Props[GraphPutFriendsActor].withRouter(FromConfig()), GraphPutFriendsActor.Id.name)
    context.actorOf(Props[LocationGraphActor].withRouter(FromConfig()), LocationGraphActor.Id.name)
    context.actorOf(Props[GraphGetFavoritesActor].withRouter(FromConfig()), GraphGetFavoritesActor.Id.name)
    context.actorOf(Props[GraphPutFavoritesActor].withRouter(FromConfig()), GraphPutFavoritesActor.Id.name)

  }

  private def startCoreIntegration() {
    println("tooe >> starting core integration...")

    context.actorOf(Props.empty.withRouter(FromConfig()), Actors.NotificationActor.name)  
    context.actorOf(Props.empty.withRouter(FromConfig()), Actors.XMPPAccountActor.name)  
  }
  
  private def startCoreProfile() {
    if(settings.withIntegrationService) {
      startCoreIntegration()
    }
    
    println("tooe >> starting core profile...")

    context.actorOf(Props[UserReadActor].withRouter(FromConfig()), UserReadActor.Id.name)
    context.actorOf(Props[UserWriteActor].withRouter(FromConfig()), UserWriteActor.Id.name)
    val userDataActor = context.actorOf(Props[UserDataActor].withRouter(FromConfig()), UserDataActor.Id.name)
    context.actorOf(Props[UserPhoneDataActor].withRouter(FromConfig()), UserPhoneDataActor.Id.name)

    context.actorOf(Props[UserEventReadActor].withRouter(FromConfig()), UserEventReadActor.Id.name)
    context.actorOf(Props[UserEventWriteActor].withRouter(FromConfig()), UserEventWriteActor.Id.name)
    context.actorOf(Props[UserEventDataActor].withRouter(FromConfig()), UserEventDataActor.Id.name)

    context.actorOf(Props[UsersGroupDataActor].withRouter(FromConfig()), UsersGroupDataActor.Id.name)
    context.actorOf(Props[UsersGroupActor].withRouter(FromConfig()), UsersGroupActor.Id.name)

    context.actorOf(Props[LocationReadActor].withRouter(FromConfig()), LocationReadActor.Id.name)
    context.actorOf(Props[LocationWriteActor].withRouter(FromConfig()), LocationWriteActor.Id.name)
    val locationDataActor = context.actorOf(Props[LocationDataActor].withRouter(FromConfig()), LocationDataActor.Id.name)

    context.actorOf(Props[PreModerationLocationDataActor].withRouter(FromConfig()), PreModerationLocationDataActor.Id.name)
    context.actorOf(Props[PreModerationLocationWriteActor].withRouter(FromConfig()), PreModerationLocationWriteActor.Id.name)
    context.actorOf(Props[PreModerationLocationReadActor].withRouter(FromConfig()), PreModerationLocationReadActor.Id.name)

    context.actorOf(Props[LocationCategoryActor].withRouter(FromConfig()), LocationCategoryActor.Id.name)
    context.actorOf(Props[LocationCategoryDataActor].withRouter(FromConfig()), LocationCategoryDataActor.Id.name)

    context.actorOf(Props[LocationPhotoAlbumReadActor].withRouter(FromConfig()), LocationPhotoAlbumReadActor.Id.name)
    context.actorOf(Props[LocationPhotoAlbumWriteActor].withRouter(FromConfig()), LocationPhotoAlbumWriteActor.Id.name)
    context.actorOf(Props[LocationPhotoAlbumDataActor].withRouter(FromConfig()), LocationPhotoAlbumDataActor.Id.name)
    context.actorOf(Props[LocationPhotoReadActor].withRouter(FromConfig()), LocationPhotoReadActor.Id.name)
    context.actorOf(Props[LocationPhotoWriteActor].withRouter(FromConfig()), LocationPhotoWriteActor.Id.name)
    context.actorOf(Props[LocationPhotoLikeDataActor].withRouter(FromConfig()), LocationPhotoLikeDataActor.Id.name)
    context.actorOf(Props[LocationPhotoCommentDataActor].withRouter(FromConfig()), LocationPhotoCommentDataActor.Id.name)
    context.actorOf(Props[LocationPhotoDataActor].withRouter(FromConfig()), LocationPhotoDataActor.Id.name)

    context.actorOf(Props[LocationSubscriptionActor].withRouter(FromConfig()), LocationSubscriptionActor.Id.name)
    context.actorOf(Props[LocationSubscriptionDataActor].withRouter(FromConfig()), LocationSubscriptionDataActor.Id.name)

    context.actorOf(Props[LocationNewsActor].withRouter(FromConfig()), LocationNewsActor.Id.name)
    context.actorOf(Props[LocationNewsDataActor].withRouter(FromConfig()), LocationNewsDataActor.Id.name)

    context.actorOf(Props[LocationNewsLikeDataActor].withRouter(FromConfig()), LocationNewsLikeDataActor.Id.name)
    context.actorOf(Props[LocationNewsLikeWriteActor].withRouter(FromConfig()), LocationNewsLikeWriteActor.Id.name)

    context.actorOf(Props[LocationsChainStatsDataActor].withRouter(FromConfig()), LocationsChainStatsDataActor.Id.name)
    context.actorOf(Props[LocationsChainDataActor].withRouter(FromConfig()), LocationsChainDataActor.Id.name)
    context.actorOf(Props[LocationsChainReadActor].withRouter(FromConfig()), LocationsChainReadActor.Id.name)
    context.actorOf(Props[LocationsChainWriteActor].withRouter(FromConfig()), LocationsChainWriteActor.Id.name)

    context.actorOf(Props[SecurityActor].withRouter(FromConfig()), SecurityActor.Id.name)

    context.actorOf(Props[ProductReadActor].withRouter(FromConfig()), ProductReadActor.Id.name)
    context.actorOf(Props[ProductWriteActor].withRouter(FromConfig()), ProductWriteActor.Id.name)
    context.actorOf(Props[ProductSnapshotDataActor].withRouter(FromConfig()), ProductSnapshotDataActor.Id.name)
    val productDataActor = context.actorOf(Props[ProductDataActor].withRouter(FromConfig()), ProductDataActor.Id.name)

    val paymentDataActor = context.actorOf(Props[PaymentDataActor].withRouter(FromConfig()), PaymentDataActor.Id.name)
    context.actorOf(Props[PaymentSystemActor].withRouter(FromConfig()), PaymentSystemActor.Id.name)

    context.actorOf(Props(
      new PaymentWorkflowActor(
        productDataActor = productDataActor,
        locationDataActor = locationDataActor,
        paymentDataActor = paymentDataActor)
      ).withRouter(FromConfig()),
      PaymentWorkflowActor.Id.name
    )

    context.actorOf(Props(new PlatronStrategyActor(userDataActor)).withRouter(FromConfig()), PlatronStrategyActor.Id.name)
    context.actorOf(Props[AlfabankStrategyActor].withRouter(FromConfig()), AlfabankStrategyActor.Id.name)
    context.actorOf(Props[TooeezzyStrategyActor].withRouter(FromConfig()), TooeezzyStrategyActor.Id.name)

    context.actorOf(Props[CheckinReadActor].withRouter(FromConfig()), CheckinReadActor.Id.name)
    context.actorOf(Props[CheckinWriteActor].withRouter(FromConfig()), CheckinWriteActor.Id.name)
    context.actorOf(Props[CheckinDataActor].withRouter(FromConfig()), CheckinDataActor.Id.name)

    context.actorOf(Props[InfoMessageActor].withRouter(FromConfig()), InfoMessageActor.Id.name)

    context.actorOf(Props[WishDataActor].withRouter(FromConfig()), WishDataActor.Id.name)
    context.actorOf(Props[WishLikeDataActor].withRouter(FromConfig()), WishLikeDataActor.Id.name)
    context.actorOf(Props[WishReadActor].withRouter(FromConfig()), WishReadActor.Id.name)
    context.actorOf(Props[WishWriteActor].withRouter(FromConfig()), WishWriteActor.Id.name)
    context.actorOf(Props[WishLikeWriteActor].withRouter(FromConfig()), WishLikeWriteActor.Id.name)
    context.actorOf(Props[WishLikeReadActor].withRouter(FromConfig()), WishLikeReadActor.Id.name)

    context.actorOf(Props[PromotionDataActor].withRouter(FromConfig()), PromotionDataActor.Id.name)
    context.actorOf(Props[PromotionVisitorDataActor].withRouter(FromConfig()), PromotionVisitorDataActor.Id.name)
    context.actorOf(Props[PromotionReadActor].withRouter(FromConfig()), PromotionReadActor.Id.name)
    context.actorOf(Props[PromotionWriteActor].withRouter(FromConfig()), PromotionWriteActor.Id.name)
    context.actorOf(Props[PromotionVisitorWriteActor].withRouter(FromConfig()), PromotionVisitorWriteActor.Id.name)
    context.actorOf(Props[PromotionVisitorReadActor].withRouter(FromConfig()), PromotionVisitorReadActor.Id.name)

    context.actorOf(Props[RegionDataActor].withRouter(FromConfig()), RegionDataActor.Id.name)
    context.actorOf(Props[RegionActor].withRouter(FromConfig()), RegionActor.Id.name)

    context.actorOf(Props[CacheSessionDataActor].withRouter(FromConfig()), CacheSessionDataActor.Id.name)
    context.actorOf(Props[CacheUserOnlineDataActor].withRouter(FromConfig()), CacheUserOnlineDataActor.Id.name)
    context.actorOf(Props[SessionActor].withRouter(FromConfig()), SessionActor.Id.name)
    context.actorOf(Props[AuthorizationActor].withRouter(FromConfig()), AuthorizationActor.Id.name)

    context.actorOf(Props[AdminSessionActor].withRouter(FromConfig()), AdminSessionActor.Id.name)

    context.actorOf(Props[CacheAdminSessionDataActor].withRouter(FromConfig()), CacheAdminSessionDataActor.Id.name)

    context.actorOf(Props[CredentialsDataActor].withRouter(FromConfig()), CredentialsDataActor.Id.name)

    context.actorOf(Props[CountryDataActor].withRouter(FromConfig()), CountryDataActor.Id.name)
    context.actorOf(Props[CountryReadActor].withRouter(FromConfig()), CountryReadActor.Id.name)

    context.actorOf(Props[MongoDataActor].withRouter(FromConfig()), name = "mongodata")

    context.actorOf(Props[FriendReadActor].withRouter(FromConfig()), FriendReadActor.Id.name)
    context.actorOf(Props[FriendWriteActor].withRouter(FromConfig()), FriendWriteActor.Id.name)

    context.actorOf(Props[FriendCacheDataActor].withRouter(FromConfig()), FriendCacheDataActor.Id.name)
    context.actorOf(Props[FriendCacheReadActor].withRouter(FromConfig()), FriendCacheReadActor.Id.name)

    context.actorOf(Props[FriendshipRequestWriteActor].withRouter(FromConfig()), FriendshipRequestWriteActor.Id.name)
    context.actorOf(Props[FriendshipRequestReadActor].withRouter(FromConfig()), FriendshipRequestReadActor.Id.name)
    context.actorOf(Props[FriendshipRequestDataActor].withRouter(FromConfig()), FriendshipRequestDataActor.Id.name)

    context.actorOf(Props[PhotoAlbumReadActor].withRouter(FromConfig()), PhotoAlbumReadActor.Id.name)
    context.actorOf(Props[PhotoAlbumWriteActor].withRouter(FromConfig()), PhotoAlbumWriteActor.Id.name)
    context.actorOf(Props[PhotoAlbumDataActor].withRouter(FromConfig()), PhotoAlbumDataActor.Id.name)
    context.actorOf(Props[PhotoReadActor].withRouter(FromConfig()), PhotoReadActor.Id.name)
    context.actorOf(Props[PhotoWriteActor].withRouter(FromConfig()), PhotoWriteActor.Id.name)
    context.actorOf(Props[PhotoDataActor].withRouter(FromConfig()), PhotoDataActor.Id.name)
    context.actorOf(Props[PhotoCommentDataActor].withRouter(FromConfig()), PhotoCommentDataActor.Id.name)
    context.actorOf(Props[PhotoLikeDataActor].withRouter(FromConfig()), PhotoLikeDataActor.Id.name)

    context.actorOf(Props[PresentReadActor].withRouter(FromConfig()), PresentReadActor.Id.name)
    context.actorOf(Props[PresentWriteActor].withRouter(FromConfig()), PresentWriteActor.Id.name)
    context.actorOf(Props[PresentDataActor].withRouter(FromConfig()), PresentDataActor.Id.name)

    context.actorOf(Props[AdminUserReadActor].withRouter(FromConfig()), AdminUserReadActor.Id.name)
    context.actorOf(Props[AdminUserWriteActor].withRouter(FromConfig()), AdminUserWriteActor.Id.name)
    context.actorOf(Props[AdminUserDataActor].withRouter(FromConfig()), AdminUserDataActor.Id.name)
    context.actorOf(Props[AdminCredentialsDataActor].withRouter(FromConfig()), AdminCredentialsDataActor.Id.name)

    context.actorOf(Props[AdminUserEventDataActor].withRouter(FromConfig()), AdminUserEventDataActor.Id.name)
    context.actorOf(Props[AdminUserEventReadActor].withRouter(FromConfig()), AdminUserEventReadActor.Id.name)
    context.actorOf(Props[AdminUserEventWriteActor].withRouter(FromConfig()), AdminUserEventWriteActor.Id.name)

    context.actorOf(Props[AdminRoleDataActor].withRouter(FromConfig()), AdminRoleDataActor.Id.name)
    context.actorOf(Props[AdminRoleActor].withRouter(FromConfig()), AdminRoleActor.Id.name)

    createMediaServerActors()

    context.actorOf(Props[MaritalStatusActor].withRouter(FromConfig()), MaritalStatusActor.Id.name)
    context.actorOf(Props[MaritalStatusDataActor].withRouter(FromConfig()), MaritalStatusDataActor.Id.name)

    context.actorOf(Props[StarsCategoriesActor].withRouter(FromConfig()), StarsCategoriesActor.Id.name)
    context.actorOf(Props[StarsCategoriesDataActor].withRouter(FromConfig()), StarsCategoriesDataActor.Id.name)

    context.actorOf(Props[StarSubscriptionActor].withRouter(FromConfig()), StarSubscriptionActor.Id.name)
    context.actorOf(Props[StarSubscriptionDataActor].withRouter(FromConfig()), StarSubscriptionDataActor.Id.name)

    context.actorOf(Props[UpdateStatisticActor].withRouter(FromConfig()), UpdateStatisticActor.Id.name)

    context.actorOf(Props[NewsDataActor].withRouter(FromConfig()), NewsDataActor.Id.name)
    context.actorOf(Props[NewsReadActor].withRouter(FromConfig()), NewsReadActor.Id.name)
    context.actorOf(Props[NewsWriteActor].withRouter(FromConfig()), NewsWriteActor.Id.name)
    context.actorOf(Props[NewsCommentWriteActor].withRouter(FromConfig()), NewsCommentWriteActor.Id.name)
    context.actorOf(Props[NewsCommentReadActor].withRouter(FromConfig()), NewsCommentReadActor.Id.name)
    context.actorOf(Props[NewsCommentDataActor].withRouter(FromConfig()), NewsCommentDataActor.Id.name)
    context.actorOf(Props[NewsLikeDataActor].withRouter(FromConfig()), NewsLikeDataActor.Id.name)

    context.actorOf(Props[CompanyDataActor].withRouter(FromConfig()), CompanyDataActor.Id.name)
    context.actorOf(Props[PreModerationCompanyDataActor].withRouter(FromConfig()), PreModerationCompanyDataActor.Id.name)
    context.actorOf(Props[CompanyReadActor].withRouter(FromConfig()), CompanyReadActor.Id.name)
    context.actorOf(Props[CompanyWriteActor].withRouter(FromConfig()), CompanyWriteActor.Id.name)
    context.actorOf(Props[ModerationCompanyWriteActor].withRouter(FromConfig()), ModerationCompanyWriteActor.Id.name)
    context.actorOf(Props[ModerationCompanyReadActor].withRouter(FromConfig()), ModerationCompanyReadActor.Id.name)

    context.actorOf(Props[CacheWriteSnifferActor].withRouter(FromConfig()), CacheWriteSnifferActor.Id.name)
    context.actorOf(Props[CacheWriteSnifferDataActor].withRouter(FromConfig()), CacheWriteSnifferDataActor.Id.name)

    context.actorOf(Props[OnlineStatusDataActor].withRouter(FromConfig()), OnlineStatusDataActor.Id.name)
    context.actorOf(Props[OnlineStatusActor].withRouter(FromConfig()), OnlineStatusActor.Id.name)

    context.actorOf(Props[CurrencyDataActor].withRouter(FromConfig()), CurrencyDataActor.Id.name)
    context.actorOf(Props[CurrencyActor].withRouter(FromConfig()), CurrencyActor.Id.name)

    context.actorOf(Props[PeriodDataActor].withRouter(FromConfig()), PeriodDataActor.Id.name)
    context.actorOf(Props[PeriodActor].withRouter(FromConfig()), PeriodActor.Id.name)

    context.actorOf(Props[LifecycleStatusDataActor].withRouter(FromConfig()), LifecycleStatusDataActor.Id.name)
    context.actorOf(Props[LifecycleStatusActor].withRouter(FromConfig()), LifecycleStatusActor.Id.name)

    context.actorOf(Props[EventGroupDataActor].withRouter(FromConfig()), EventGroupDataActor.Id.name)
    context.actorOf(Props[EventGroupActor].withRouter(FromConfig()), EventGroupActor.Id.name)

    context.actorOf(Props[EventTypeDataActor].withRouter(FromConfig()), EventTypeDataActor.Id.name)

    context.actorOf(Props[ModerationStatusDataActor].withRouter(FromConfig()), ModerationStatusDataActor.Id.name)
    context.actorOf(Props[ModerationStatusActor].withRouter(FromConfig()), ModerationStatusActor.Id.name)

    context.actorOf(Props[FavoriteStatsActor].withRouter(FromConfig()), FavoriteStatsActor.Id.name)
    context.actorOf(Props[FavoriteStatsDataActor].withRouter(FromConfig()), FavoriteStatsDataActor.Id.name)

    context.actorOf(Props[UrlsDataActor].withRouter(FromConfig()), UrlsDataActor.Id.name)
    context.actorOf(Props[UrlsWriteActor].withRouter(FromConfig()), UrlsWriteActor.Id.name)

    context.actorOf(Props[CalendarEventDataActor].withRouter(FromConfig()), CalendarEventDataActor.Id.name)
    context.actorOf(Props[CalendarEventWriteActor].withRouter(FromConfig()), CalendarEventWriteActor.Id.name)
    context.actorOf(Props[CalendarEventReadActor].withRouter(FromConfig()), CalendarEventReadActor.Id.name)

    context.actorOf(Props[FreePresentWriteActor].withRouter(FromConfig()), FreePresentWriteActor.Id.name)
    context.actorOf(Props[WelcomePresentWriteActor].withRouter(FromConfig()), WelcomePresentWriteActor.Id.name)

    context.actorOf(Props[PaymentResultUrlReadActor].withRouter(FromConfig()), PaymentResultUrlReadActor.Id.name)

    context.actorOf(Props[OrderReadActor].withRouter(FromConfig()), OrderReadActor.Id.name)
    context.actorOf(Props[OrderWriteActor].withRouter(FromConfig()), OrderWriteActor.Id.name)

    if(settings.Job.UrlCheck.enable) {
      val urlCheckJob = context.actorOf(Props[UrlsCheckJobActor].withRouter(FromConfig()), UrlsCheckJobActor.Id.name)
      context.actorOf(Props[CdnUrlCheckerActor].withRouter(FromConfig()), CdnUrlCheckerActor.Id.name)
      context.actorOf(Props[PingCdnUrlActor].withRouter(FromConfig()), PingCdnUrlActor.Id.name)
      context.actorOf(Props[UploadHttpImageActor].withRouter(FromConfig()), UploadHttpImageActor.Id.name)
      context.actorOf(Props[UrlTypeChangeActor].withRouter(FromConfig()), UrlTypeChangeActor.Id.name)

      import scala.concurrent.ExecutionContext.Implicits.global
      settings.Job.UrlCheck.types.foreach { urlType =>
        context.system.scheduler.schedule(settings.Job.UrlCheck.delay, settings.Job.UrlCheck.interval, urlCheckJob, UrlsCheckJobActor.CheckUrls(urlType))
      }
    }

    if (settings.Job.OverduePaymentCancellation.enable) {
      scheduleOverduePaymentCancellationJob()
    }

    if (settings.Job.ExpiredPresentsMarker.enable) {
      scheduleExpiredPresentsMarkerJob()
    }
  }

  def scheduleOverduePaymentCancellationJob(): Unit = {
    import settings.Job.OverduePaymentCancellation._
    val props = Props[OverduePaymentCancellationActor].withRouter(FromConfig())
    val jobActorRef = context.actorOf(props, OverduePaymentCancellationActor.Id.name)
    val startJobMsg = OverduePaymentCancellationActor.StartJob()
    context.system.scheduler.schedule(initialDelay, interval, jobActorRef, startJobMsg)(context.dispatcher)
    log.info("OverduePaymentCancellation job has been scheduled")
  }

  def scheduleExpiredPresentsMarkerJob(): Unit = {
    import settings.Job.ExpiredPresentsMarker._
    val props = Props[ExpiredPresentsMarkerActor].withRouter(FromConfig())
    val jobActorRef = context.actorOf(props, ExpiredPresentsMarkerActor.Id.name)
    context.system.scheduler.schedule(initialDelay, interval, jobActorRef, ExpiredPresentsMarkerActor.CheckPresents)(context.dispatcher)
    log.info("ExpiredPresentsMarker job has been scheduled")
  }

  def createMediaServerActors(): Unit = {
    val isMockMediaServer = context.system.settings.config.getBoolean("mediaserver.mock")
    log.info("Mock MediaServer = {}", isMockMediaServer)
    if (isMockMediaServer) {
      context.actorOf(Props[UploadMediaServerMockActor].withRouter(FromConfig()), UploadMediaServerActor.Id.name)
      context.actorOf(Props[NullActor].withRouter(FromConfig()), DeleteMediaServerActor.Id.name)
      context.actorOf(Props[NullActor].withRouter(FromConfig()), CreateLinkMediaServerActor.Id.name)
    }
    else {
      context.actorOf(Props[UploadMediaServerActor].withRouter(FromConfig()), UploadMediaServerActor.Id.name)
      context.actorOf(Props[DeleteMediaServerActor].withRouter(FromConfig()), DeleteMediaServerActor.Id.name)
      context.actorOf(Props[CreateLinkMediaServerActor].withRouter(FromConfig()), CreateLinkMediaServerActor.Id.name)
    }
  }

  private def startMigrationProfile() {
    println("tooe >> starting migration profile...")

    context.actorOf(Props[IdMappingDataActor].withRouter(FromConfig()), IdMappingDataActor.Id.name)
    context.actorOf(Props[DictionaryIdMappingDataActor].withRouter(FromConfig()), DictionaryIdMappingDataActor.Id.name)
    context.actorOf(Props[CompanyMigratorActor].withRouter(FromConfig()), CompanyMigratorActor.Id.name)
    context.actorOf(Props[AdminUserMigratorActor].withRouter(FromConfig()), AdminUserMigratorActor.Id.name)
    context.actorOf(Props[DictionaryIdMappingActor].withRouter(FromConfig()), DictionaryIdMappingActor.Id.name)
    context.actorOf(Props[UserMigratorActor].withRouter(FromConfig()), UserMigratorActor.Id.name)
    context.actorOf(Props[PhotoMigratorActor].withRouter(FromConfig()), PhotoMigratorActor.Id.name)
    context.actorOf(Props[LocationMigratorActor].withRouter(FromConfig()), LocationMigratorActor.Id.name)
    context.actorOf(Props[StarSubscriptionMigrator].withRouter(FromConfig()), StarSubscriptionMigrator.Id.name)
    context.actorOf(Props[ProductMigratorActor].withRouter(FromConfig()), ProductMigratorActor.Id.name)
    context.actorOf(Props[LocationPhotoMigratorActor].withRouter(FromConfig()), LocationPhotoMigratorActor.Id.name)
    context.actorOf(Props[WishMigratorActor].withRouter(FromConfig()), WishMigratorActor.Id.name)
    context.actorOf(Props[PresentMigratorActor].withRouter(FromConfig()), PresentMigratorActor.Id.name)
    context.actorOf(Props[PromotionMigratorActor].withRouter(FromConfig()), PromotionMigratorActor.Id.name)
    context.actorOf(Props[FavoriteMigratorActor].withRouter(FromConfig()), FavoriteMigratorActor.Id.name)
    context.actorOf(Props[FriendshipRequestMigratorActor].withRouter(FromConfig()), FriendshipRequestMigratorActor.Id.name)
    context.actorOf(Props[LocationNewsMigratorActor].withRouter(FromConfig()), LocationNewsMigratorActor.Id.name)
    context.actorOf(Props[LocationSubscriptionMigratorActor].withRouter(FromConfig()), LocationSubscriptionMigratorActor.Id.name)
    context.actorOf(Props[LocationChainMigratorActor].withRouter(FromConfig()), LocationChainMigratorActor.Id.name)
    context.actorOf(Props[FriendshipMigratorActor].withRouter(FromConfig()), FriendshipMigratorActor.Id.name)
    context.actorOf(Props[NewsMigratorActor].withRouter(FromConfig()), NewsMigratorActor.Id.name)
    context.actorOf(Props[UserEventMigratorActor].withRouter(FromConfig()), UserEventMigratorActor.Id.name)
  }
}