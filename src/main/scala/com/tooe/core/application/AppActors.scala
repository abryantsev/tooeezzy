package com.tooe.core.application

import akka.actor.ActorRef
import akka.actor.ActorRefFactory

trait AppActors {

  def lookup(actorId: Symbol)(implicit system: ActorRefFactory): ActorRef =
    system.actorFor(s"/user/application/${actorId.name}")

//  def lookupRouter(routerId: Symbol)(implicit system: ActorRefFactory): ActorRef =
//    system.actorFor(s"/user/${routerId.name}")
}

object Actors {
  val ProductRead = 'productRead
  val ProductWrite = 'productWrite
  val ProductData = 'productData
  val LocationRead = 'locationRead
  val LocationWrite = 'locationWrite
  val LocationData = 'locationData
  val PreModerationLocationData = 'preModerationLocationData
  val PreModerationLocationRead = 'preModerationLocationRead
  val PreModerationLocationWriteActor = 'preModerationLocationWrite
  val LocationGraph = 'locationGraph
  val GetFavoriteLocationsGraph = 'getFavoriteLocationsGraph
  val PutLocationToFavoriteGraph = 'putLocationToFavoriteGraph
  val LocationPhotoAlbumRead = 'locationPhotoAlbumRead
  val LocationPhotoAlbumWrite = 'locationPhotoAlbumWrite
  val LocationPhotoAlbumData = 'locationPhotoAlbumData
  val LocationCategory = 'locationCategory
  val LocationCategoryData = 'locationCategoryData
  val LocationPhotoRead = 'locationPhotoRead
  val LocationPhotoWrite = 'locationPhotoWrite
  val LocationPhotoData = 'locationPhotoData
  val LocationPhotoLikeData = 'locationPhotoLikeData
  val LocationPhotoCommentData = 'locationPhotoCommentData
  val LocationSubscription = 'locationSubscription
  val LocationSubscriptionData = 'locationSubscriptionData
  val LocationNews = 'locationNews
  val LocationNewsData = 'locationNewsData
  val Security = 'security
  val UserRead = 'userRead
  val UserWrite = 'userWrite
  val UserData = 'userData
  val UserCache = 'userCache
  val UserGraph = 'userGraph
  val UserPhoneData = 'userPhoneData

  val FriendRead = 'friendRead
  val FriendWrite = 'friendWrite

  val FriendDataCache = 'friendDataCache
  val FriendReadCache = 'friendReadCache

  val GetFriendsGraph = 'graphGetFriends
  val PutFriendsGraph = 'graphPutFriends
  val TitanLoadGraph = 'titanLoadGraph

  val FriendshipRequestWrite = 'friendshipRequestWrite
  val FriendshipRequestRead = 'friendshipRequestRead

  val UserEventRead = 'userEventRead
  val UserEventWrite = 'userEventWrite
  val UserEventData = 'userEventData
  val UsersGroupData = 'usersGroupData
  val UsersGroup = 'usersGroup

  val PaymentWorkflow = 'paymentWorkflow
  val PaymentData = 'paymentData
  val PlatronPaymentSystem = 'platronPaymentSystem

  val PlatronStrategy = 'platronStrategy
  val AlfabankStrategy = 'alfabankStrategy
  val TooeezzyStrategy = 'tooeezzyStrategy

  val Region = 'region
  val RegionData = 'regionData
  val CountryRead = 'country
  val CountryData = 'countryData
  val CheckinRead = 'checkinRead
  val CheckinWrite = 'checkinWrite
  val CheckinData = 'checkinData
  val InfoMessage = 'infoMessage

  val PromotionData = 'promotionData
  val PromotionVisitorData = 'promotionVisitorData
  val PromotionRead = 'promotionRead
  val PromotionWrite = 'promotionWrite

  val PromotionVisitorWrite = 'promotionVisitorWrite
  val PromotionVisitorRead = 'promotionVisitorRead

  val CompanyRead = 'companyRead

  val CacheSessionData = 'cacheSessionData
  val CacheWriteSniffer = 'cacheWriteSniffer
  val CacheWriteSnifferData = 'cacheWriteSnifferData
  val CacheUserOnlineData = 'cacheUserOnlineData
  val Session = 'session
  val AdminSession = 'adminSession
  val CacheAdminSessionData = 'cacheAdminSessionData
  val CredentialsData = 'credentialsData
  val AuthorizationActor = 'authorizationActor

  val PhotoRead = 'photoRead
  val PhotoWrite = 'photoWrite
  val PhotoData = 'photoData
  val PhotoCommentData = 'photoCommentData
  val PhotoAlbumRead = 'photoAlbumRead
  val PhotoAlbumWrite = 'photoAlbumWrite
  val PhotoAlbumData = 'photoAlbumData
  val PhotoLikeDate = 'photoLikeData

  val PresentRead = 'presentRead
  val PresentWrite = 'presentWrite
  val PresentData = 'presentData

  val UploadMediaServer = 'uploadMediaServer
  val DeleteMediaServer = 'deleteMediaServer
  val CreateLinkMediaServer = 'createLinkMediaServer

  val Currency = 'currency
  val CurrencyData = 'currencyData

  val Period = 'period
  val PeriodData = 'periodData

  val MaritalStatus = 'maritalStatus
  val MaritalStatusData = 'maritalStatusData

  val ModerationStatus = 'moderationStatus
  val ModerationStatusData = 'moderationStatusData

  val StarSubscription = 'starSubscription
  val StarSubscriptionData = 'starSubscriptionData
  val StarsCategories = 'starsCategories
  val StarsCategoriesData = 'starsCategoriesData

  val StatusActor = 'status
  val StatusDataActor = 'statusData

  val NewsWrite = 'newsWrite
  val NewsRead = 'newsRead
  val NewsData = 'newsData
  val NewsCommentWriteActor = 'newsCommentWrite
  val NewsCommentReadActor = 'newsCommentRead
  val NewsCommentDataActor = 'newsCommentData
  val NewsLikeDataActor = 'newsLikeDataActor

  val WishRead = 'wishRead
  val WishWrite = 'wishWrite
  val WishData = 'wishData

  val WishLikeData = 'wishLikeData
  val WishLikeWrite = 'wishLikeWrite
  val WishLikeRead = 'wishLikeRead

  val UpdateStatistic = 'updateStatistic

  val CompanyWriteActor = 'companyWrite
  val CompanyDataActor = 'companyData
  val PreModerationCompanyDataActor = 'preModerationCompanyData
  val ModerationCompanyWriteActor = 'moderationCompanyWrite
  val ModerationCompanyReadActor = 'moderationCompanyRead

  val AdminUserWrite = 'adminUserWrite
  val AdminUserRead = 'adminUserRead
  val AdminUserData = 'adminUserData
  val AdminCredentialsData = 'adminCredentialsData
  val EventTypeDataActor = 'eventTypeData
  val LocationStatisticsDataActor = 'locationStatisticsTypeData

  val LocationNewsLikeWrite = 'locationNewsLikeWrite
  val LocationNewsLikeData = 'locationNewsLikeData

  val FriendshipRequestData = 'friendshipRequestData

  val OnlineStatusData = 'onlineStatusData
  val OnlineStatus = 'onlineStatus

  val AdminRoleData = 'adminRoleData
  val AdminRole = 'adminRole

  val LifecycleData = 'lifecycleData
  val Lifecycle = 'lifecycle

  val EventGroupData = 'eventGroupData
  val EventGroup = 'eventGroup

  val AdminEventData = 'adminEventData
  val AdminEventRead = 'adminEventRead
  val AdminEventWrite = 'adminEventWrite

  val LocationsChainStatsData = 'locationsChainStatsData
  val LocationsChainData = 'locationsChainData
  val LocationsChainRead = 'locationsChainRead
  val LocationsChainWrite = 'locationsChainWrite

  val ProductSnapshotData = 'productSnapshotData

  val FavoriteStatsActor = 'favoriteStatsActor
  val FavoriteStatsDataActor = 'favoriteStatsDataActor

  val UrlsData = 'urlsData
  val UrlsWrite = 'urlsWrite

  val CDNCheckActor = 'cdnCheckActor
  val XMPPAccountActor = 'xmppAccountActor
  val NotificationActor = 'notificationActor

  val urlsCheckJob = 'urlCheckJob
  val s3UrlChecker = 's3UrlChecker
  val pingCdnUrl = 'pingCdnUrl
  val uploadHttpImage = 'uploadHttpImage
  val urlTypeChange = 'urlTypeChange

  val CalendarEventData = 'calendarEventData
  val CalendarEventWrite = 'calendarEventWrite
  val CalendarEventRead = 'calendarEventRead

  val ExpiredPresentsMarker = 'expiredPresentsMarker

  val OverduePaymentCancellation = 'overduePaymentCancellation
  val FreePresentWrite = 'freePresentWrite
  val WelcomePresentWrite = 'welcomePresentWrite

  val PaymentResultUrlRead = 'paymentResultUrlRead

  val OrderRead = 'orderRead
  val OrderWrite = 'orderWrite
}