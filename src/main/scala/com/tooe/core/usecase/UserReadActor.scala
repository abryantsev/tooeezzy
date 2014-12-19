package com.tooe.core.usecase

import Implicits._
import com.fasterxml.jackson.annotation.JsonProperty
import com.tooe.api.JsonProp
import com.tooe.api.service._
import com.tooe.api.validation.{Validatable, ValidationContext}
import com.tooe.core.application.Actors
import com.tooe.core.db.graph.GraphGetFriendsActor
import com.tooe.core.db.graph.domain.FriendshipType
import com.tooe.core.db.graph.msg.{GraphFriends, GraphGetMutualFriends}
import com.tooe.core.db.mongo.domain.UserMedia.CDNDynamic
import com.tooe.core.db.mongo.domain._
import com.tooe.core.db.mongo.domain.Photo
import com.tooe.core.domain.UserFields.{Users, UsersCount}
import com.tooe.core.domain._
import com.tooe.core.usecase.online_status.OnlineStatusDataActor
import com.tooe.core.usecase.session.CacheUserOnlineDataActor
import com.tooe.core.usecase.star_category.StarsCategoriesDataActor
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor
import com.tooe.core.usecase.star_subscription.StarSubscriptionDataActor.ExistSubscribe
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.user.UserDataActor._
import com.tooe.core.util.MediaHelper._
import com.tooe.core.util.{Images, InfoMessageHelper, Lang}
import java.util.Date
import scala.Some
import scala.collection.JavaConverters._
import scala.concurrent.Future
import user.response.Author
import com.tooe.core.exceptions.UserNotFoundException
import com.tooe.core.main.SharedActorSystem

object UserReadActor{
  final val Id = Actors.UserRead

  case class SelectAuthors(userIds: Seq[UserId], imageDimension: String)
  case class SearchUsers(request: SearchUsersRequest)
  case class SearchStars(request: SearchStarsRequest, offsetLimit: OffsetLimit, ctx: RouteContext)
  case class GetSearchUsers(userIds: Seq[UserId])
  case class GetSearchUsersDto(userIds: Seq[UserId], imageSize: String)
  case class GetUserInfo(r: GetUserInfoRequest, userId: UserId, lang: Lang)
  case class GetUserMoreInfo(userId: UserId)
  case class GetMutualFriends(userId: UserId, currentUserId: UserId)
  case class GetUsersMainScreen(request:GetUsersMainScreenRequest, userId: UserId, ctx: RouteContext)
  case class GetOwnStatistics(request: GetUsersOwnStatisticsRequest, currentUserId: UserId, lang: Lang)
  case class GetOtherUserStatistics(request: GetOtherUserStatisticsRequest, lang: Lang)
  case class FindActors(ids: Set[UserId], lang: Lang, imageSize: String)
  case class FindActorsShort(ids: Set[UserId], lang: Lang, imageSize: String)
  case class GetUserProfile(userId: UserId, viewType: ViewType, ctx: RouteContext)
  case class GetStar(userId: UserId, starId: UserId, viewType: ShowType)
  case class GetAuthorDetailsByIds(userIds: Seq[UserId], imageDimension: String)
  case class GetUserInfoShortItems(userIds: Seq[UserId], imageSize: String)
  case class GetCheckedInUsers(ids: Seq[UserId], imageSize: String)
  case class GetLocationCheckinInfoItem(allUserIds: Seq[UserId], currentUserId: UserId)
  case class GetUserWishesCount(userId: UserId, fulfilledOnly: Boolean)
  case class GetUsersOnlineStatuses(userIds: Option[Set[UserId]], currentUserId: UserId, lang: Lang)
  case class GetSubscriberStarsItems(userIds: Seq[UserId])
  case class GetSubscriberStarsItemsDto(userIds: Seq[UserId])
  case class GetPaymentUsers(ids: Seq[UserId])
  case class GetStarItems(ids: Seq[UserId])
  case class GetStarStatistics(starId: UserId, fields: Option[Set[StarStatisticField]])
  case class GetUserStarSubscriptionsItems(ids: Seq[UserId])
  case object GetTopStars
}

class UserReadActor extends AppActor with ExecutionContextProvider with FriendReadComponent {

  lazy val userDataActor = lookup(UserDataActor.Id)
  lazy val photoReadActor = lookup(PhotoReadActor.Id)
  lazy val maritalStatusActor = lookup(MaritalStatusActor.Id)
  lazy val regionActor = lookup(RegionActor.Id)
  lazy val countryActor = lookup(CountryReadActor.Id)
  lazy val starSubscriptionDataActor = lookup(StarSubscriptionDataActor.Id)
  lazy val starsCategoriesDataActor = lookup(StarsCategoriesDataActor.Id)
  lazy val photoDataActor = lookup(PhotoDataActor.Id)
  lazy val cacheUserOnlineDataActor = lookup(CacheUserOnlineDataActor.Id)
  lazy val onlineStatusDataActor = lookup(OnlineStatusDataActor.Id)
  lazy val graphGetFriendsDataActor = lookup(GraphGetFriendsActor.Id)
  lazy val friendshipRequestReadActor = lookup(FriendshipRequestReadActor.Id)

  import UserReadActor._

  def receive = {
    case GetMutualFriends(userId, currentUserId) =>
      (for {
        fiendsIds <- graphGetFriendsDataActor.ask(new GraphGetMutualFriends(userId, currentUserId)).mapTo[GraphFriends].map(_.getFriends.asScala.toSeq)
        friends <- getUsers(fiendsIds)
      } yield GetMutualFriendsResponse(friends.size, friends.map(friend => MutualFriendResponseItem(friend)))
      ) pipeTo sender
    case GetUsersOnlineStatuses(userIds, currentUserId, lang) =>
      val usersIds = userIds.getOrElse(Set(currentUserId)).toSeq
      implicit val lng = lang
      val future = for {
        cacheOnlineStatuses <- getUsersCacheOnlineStatuses(usersIds)
        onlineStatuses <- getOnlineStatuses((OnlineStatusId.Offline :: cacheOnlineStatuses.values.toList).toSet).map(_.map(OnlineStatusResponseItem(_)))
        users <- getUsers(usersIds)
        userMap = users.toMapId(_.id)
        onlineStatusesMap = onlineStatuses.toMapId(_.id)
        userIdsByStatus = usersIds.foldLeft(cacheOnlineStatuses){
          case (map, userId) if map.contains(userId) => map
          case (map, userId) => map + (userId -> OnlineStatusId.Offline)
        }
      }  yield {
        val diff = usersIds.diff(users.map(_.id))
        if(diff.nonEmpty) throw UserNotFoundException("Users with ids: " + diff.map(_.id).mkString(",") + " not found")
        else GetUsersOnlineStatusesResponse(users.map(userId => GetUsersOnlineStatusesResponseItem(userMap(userId.id), onlineStatusesMap(userIdsByStatus(userId.id)))))
      }
      future pipeTo sender
    case GetOtherUserStatistics(request, lang) =>
          implicit val lng = lang
      import UserStatisticViewType._
      val future = getUser(request.userId).flatMap(user =>
        request.statisticsView match {
          case FriendsOnlineBlock =>
            getOnlineFriendsNumber(user.id) map { onlineFriendsNumber =>
              GetOtherUserStatisticsResponseContent(friendsOnlineCount = Some(onlineFriendsNumber))
            }
          case FullBlock => Future(GetOtherUserStatisticsResponseContent(friendsCount = user.statistics.friendsCount.optionNonZero,
            wishesCount = user.statistics.wishesCount.optionNonZero,
            photoAlbumsCount = user.statistics.photoAlbumsCount.optionNonZero,
            favoriteLocationsCount = user.statistics.favoriteLocationsCount.optionNonZero
          ))
          case other =>
            def take(viewType: UserStatisticViewType, count: Int) = {
              if (other.asInstanceOf[Set[UserStatisticViewType]].contains(viewType)) count.optionNonZero else None
            } //cast is to avoid type inference issue

            Future(GetOtherUserStatisticsResponseContent(
              friendsCount = take(Friends, user.statistics.friendsCount),
              wishesCount = take(Wishes, user.statistics.wishesCount),
              photoAlbumsCount = take(PhotoAlbums, user.statistics.photoAlbumsCount),
              favoriteLocationsCount = take(Favorites, user.statistics.favoriteLocationsCount)))
        }).map(content => GetOtherUserStatisticResponse(content))
      future pipeTo sender

    case GetOwnStatistics(request, currentUserId, lang) =>
      implicit val lng = lang
      implicit val fields = request.statisticsView
      getUser(currentUserId).flatMap {
        case user if fields.contains(UsersOwnStatisticsViewType.FriendsOnline) =>
          getOnlineFriendsNumber(user.id).map(GetUsersOwnStatisticsResponseItem(user, _))
        case user =>
          Future.successful(GetUsersOwnStatisticsResponseItem(user, 0))
      }.map(GetUsersOwnStatisticsResponse).pipeTo(sender)

    case GetUsersMainScreen(request, userId, ctx) =>
      implicit val lang = ctx.lang
      val result = request.viewType.getOrElse(ViewTypeEx.None) match {
        case ViewTypeEx.Short =>
          getUser(userId).map(user => GetUsersMainScreenResponse(UserMainScreenShortItem(user)))
        case ViewTypeEx.Mini =>
          getUser(userId).map(user => GetUsersMainScreenResponse(UserMainScreenMiniItem(user)))
        case ViewTypeEx.None =>
          for {
            user <- getUser(userId)
            starCategories <- user.star.map(_.starCategoryIds).map(ids => getStarCategories(ids)).getOrElse(Future(Nil))
            lastPhotos <- getLastSixPhotos(userId)
            maritalStatusInfoOpt <- user.maritalStatusId.map(id =>
              getMaritalStatusInfo(id, user.gender, lang)).getOrElse(Future{ None})
          } yield GetUsersMainScreenResponse(UserMainScreenFullItem(user, maritalStatusInfoOpt, starCategories, lastPhotos))
      }
      result pipeTo sender

    case GetAuthorDetailsByIds(userIds, imageDimension) => getUsers(userIds) map (getAuthorDetails(_, imageDimension)) pipeTo sender

    case GetUserInfoShortItems(userIds, imageSize) => getUsers(userIds) map getUserInfoShortItems(imageSize) pipeTo sender

    case GetUserInfo(request, currentUserId, lang) =>
      getUser(request.userId).flatMap(user => request.viewType match {
        case ViewTypeEx.None =>
          for {
            (maritalStatus, isFriends, lastSixPhotos, friendshipStatus, friendshipGroups) <- getAdditionalUsersData(user, lang, currentUserId)
          } yield UserInfoResponse(UserInfoFullResponseContent(lastSixPhotos, maritalStatus, isFriends, friendshipStatus, friendshipGroups)(user))
        case ViewTypeEx.Short =>
          for {
            (maritalStatus, isFriends, lastSixPhotos, friendshipStatus, friendshipGroups) <- getAdditionalUsersData(user, lang, currentUserId)
          } yield UserInfoResponse(UserInfoShortResponseContent(lastSixPhotos, maritalStatus, isFriends, friendshipStatus, friendshipGroups)(user))
        case ViewTypeEx.Mini => Future(UserInfoResponse(UserInfoMiniResponseContent(user)))
      }) pipeTo sender

    case GetUserMoreInfo(userId) => getUser(userId) map GetUserMoreInfoResponse.apply pipeTo sender

    case SelectAuthors(ids, imageDimension) => getUsers(ids) map (users => users.map(Author(_, imageDimension))) pipeTo sender

    case SearchUsers(request) =>
      lazy val usersFtr = userDataActor.ask(UserDataActor.SearchUsers(request)).mapTo[Seq[User]]

      lazy val usersQuantityFtr = userDataActor.ask(UserDataActor.SearchUsersCount(request)).mapTo[Int] map (_.optionNonZero)

      val result = for {
        (users, usersCount) <- (if(request.usersAreShown) usersFtr else Future.successful(Nil))
                                  .zip(if(request.usersQuantityIsShown) usersQuantityFtr else Future.successful(None))
      } yield SearchUsersResponse(users, usersCount)
      result pipeTo sender

    case SearchStars(request, offsetLimit, ctx) =>
      val result = for {
        (stars, starsCount) <- userDataActor.ask(UserDataActor.SearchStars(request, offsetLimit)).mapTo[Seq[User]]
          .zip(userDataActor.ask(UserDataActor.SearchStarsCount(request)).mapTo[Long])
        starCategories <- (starsCategoriesDataActor ? StarsCategoriesDataActor.GetCategoriesBy(stars.map(_.star.map(_.starCategoryIds).getOrElse(Nil)).flatten)).mapTo[Seq[StarCategory]]
        starCategoriesMap = starCategories.map(cat => cat.id -> cat.name.localized(ctx.lang)).toMap
      } yield SearchStarsResponse(stars, starsCount, starCategoriesMap)
      result pipeTo sender

    case FindActors(ids, lang, imageSize) =>
      val future = for {
        users <- getUsers(ids.toSeq)
        maritalStatusIds = users.flatMap(_.maritalStatusId).toSet
        maritalStatusFun <- getMaritalStatusInfos(maritalStatusIds, lang) map getMaritalStatusInfoMap
      } yield users map user.response.ActorItem(maritalStatusFun, imageSize)
      future pipeTo sender

    case FindActorsShort(ids, lang, imageSize) =>
      val future = for {
        users <- getUsers(ids.toSeq)
        maritalStatusIds = users.flatMap(_.maritalStatusId).toSet
        maritalStatusFun <- getMaritalStatusInfos(maritalStatusIds, lang) map getMaritalStatusInfoMap
      } yield users map user.response.ActorShortItem(maritalStatusFun, imageSize)
      future pipeTo sender

    case GetUserProfile(userId, viewType, ctx) =>
      val result = for {
        user <- getUser(userId)
        (region, country) <- getRegionItem(user.contact.address.regionId, ctx.lang) zip getCountryItem(user.contact.address.countryId, ctx.lang)
        maritalStatusInfo <- user.maritalStatusId.map(id => maritalStatusActor.ask(MaritalStatusActor.GetMaritalStatusInfo(id, ctx.lang)).mapTo[Option[MaritalStatusInfo]]).getOrElse(Future(None))
      } yield viewType match {
          case ViewType.Short => UserProfileShortResponse(UserProfileShortItem(region, country, maritalStatusInfo)(user))
          case ViewType.None => UserProfileFullResponse(UserProfileFullItem(region, country, maritalStatusInfo)(user))
        }
      result pipeTo sender

    //TODO doesn't support full(none) format
    case GetStar(userId, starId, viewType) =>
      val result = for {
        (star, selfSubscribed) <- (getUser(starId) zip starSubscriptionDataActor ? ExistSubscribe(starId, userId)).mapTo[(User, Boolean)]
        mediaUrls = if(viewType != ShowType.Mini)
            star.userMedia.map(m => ImageInfo(m.url.asUrl(""), ImageType.avatar, starId.id))
          else
            Seq(star.toImageInfo(""))
        lastPhotos <- if(viewType != ShowType.Mini) getPhotoMediaUrls(star.lastPhotos) else Future successful Nil
      } yield {
        viewType match {
          case ShowType.Mini => GetStarMiniResponse(StarMiniDetailsItem(star.userMedia)(star))
          case _ => GetStarResponse(StarDetailsItem(selfSubscribed, lastPhotos, star.userMedia)(star))
        }
      }
      result pipeTo sender

    case GetCheckedInUsers(userIds, imageSize) => getUsers(userIds).map(_.map(CheckedInUser.apply(_ , imageSize))).pipeTo(sender)

    case GetSubscriberStarsItems(userIds) => getUsers(userIds) map (_ map SubscriberStarsItem.apply) pipeTo sender

    case GetSubscriberStarsItemsDto(userIds) => getUsers(userIds) map (_ map SubscriberStarsItemDto.apply) pipeTo sender

    case GetLocationCheckinInfoItem(allCheckedInUserIds, currentUserId) =>
      val future = for {
        friendIds <- getUserFriends(currentUserId)
        allCheckedInUsers <- getUsers(allCheckedInUserIds)
        (checkedInFriends, checkedInOthers) = allCheckedInUsers partition (friendIds contains _.id)
      } yield LocationCheckinInfoItem(
        checkedInFriends = checkedInFriends,
        checkedInOthers = checkedInOthers
      )
      future pipeTo sender

    case GetSearchUsers(userIds) =>
      getSearchUserDto(userIds, Images.Userssearch.Full.User.Media) map { users => users map SearchUsersItem.apply } pipeTo sender

    case GetSearchUsersDto(userIds, imageSize) => getSearchUserDto(userIds, imageSize) pipeTo sender

    case GetUserWishesCount(userId, fulfilledOnly) =>
      (userDataActor ? GetUserStatistics(userId)).mapTo[UserStatistics].map { userStatistic =>
        val count = if(fulfilledOnly) userStatistic.fulfilledWishesCount else userStatistic.wishesCount
        Some(count)
      } pipeTo sender

    case GetPaymentUsers(ids) => getUsers(ids).map(users => users.map(PaymentUser.apply)) pipeTo sender

    case GetStarItems(ids) => getUsers(ids) map (_.map(StarItem.apply)) pipeTo sender

    case GetStarStatistics(id, fields) => getUser(id).map(StarStatisticsResponse.apply(_, fields.map(_.toSeq).getOrElse(StarStatisticField.values))) pipeTo sender

    case GetUserStarSubscriptionsItems(ids) => getUsers(ids).map(_.map(StarSubscriptionItem(_))) pipeTo sender

    case GetTopStars =>
      (userDataActor ? UserDataActor.FindTopStars).mapTo[Seq[User]].map { users =>
        TopStarsSearchResponse(users.map(StarSubscriptionItem(_)))
      } pipeTo sender
  }

  def getSearchUserDto(userIds: Seq[UserId], imageSize: String): Future[Seq[SearchUsersItemDto]] =
    getUsers(userIds) map { users => users map SearchUsersItemDto(imageSize) }

  def getOnlineStatuses(onlineStatuses: Set[OnlineStatusId]): Future[Seq[OnlineStatus]] = {
    onlineStatusDataActor.ask(OnlineStatusDataActor.GetStatuses(onlineStatuses.toSeq)).mapTo[Seq[OnlineStatus]]
  }

  def getUsersCacheOnlineStatuses(userIds: Seq[UserId]): Future[Map[UserId, OnlineStatusId]] = {
    cacheUserOnlineDataActor.ask(CacheUserOnlineDataActor.GetUsersStatuses(userIds)).mapTo[Map[UserId, OnlineStatusId]]
  }

  def getFriendshipStatus(currentUserId: UserId, userId: UserId): Future[Option[FriendshipStatus]] =
    friendshipRequestReadActor.ask(FriendshipRequestReadActor.GetFriendshipInvitationStatus(actorId = currentUserId, userId = userId)).mapTo[Option[FriendshipStatus]]

  def getAdditionalUsersData(user: User, lang: Lang, currentUserId: UserId) = {
    for {
      ms <- getMaritalStatusInfoOpt(user, lang)
      isFriends <- areFriends(currentUserId, user.id)
      lastPhotos <- getPhotoMediaUrls(user.lastPhotos)
      friendshipStatus <- if (isFriends) Future(Some(FriendshipStatus.Friend)) else getFriendshipStatus(currentUserId, user.id)
      friendshipGroups <- if (isFriends) getFriendshipGroups(currentUserId, user.id).map(groups => if(groups.isEmpty) None else Some(groups.toSeq))
                          else Future successful None
    } yield (ms, isFriends, lastPhotos, friendshipStatus, friendshipGroups)
  }

  def getMaritalStatusInfoOpt(user: User, lang: Lang): Future[Option[MaritalStatusInfo]] =
    user.maritalStatusId.map(id => getMaritalStatusInfo(id, user.gender, lang)) getOrElse Future.successful(None)

  def getLastSixPhotos(userId: UserId): Future[List[Photo]] =
    photoDataActor.ask(PhotoDataActor.GetLastUserPhotos(userId)).mapTo[List[Photo]]

  def getStarCategories(categoriesIds: Seq[StarCategoryId]): Future[Seq[StarCategory]] =
    starsCategoriesDataActor.ask(StarsCategoriesDataActor.GetCategoriesBy(categoriesIds)).mapTo[Seq[StarCategory]]

  def getAuthorDetails(users: Seq[User], imageDimension: String): Seq[AuthorDetails] =
    users.map(author =>
      AuthorDetails(author, imageDimension)
    )

  def getRegionItem(id: RegionId, lang: Lang): Future[RegionItem] =
    (regionActor ? RegionActor.GetRegionItem(id, lang)).mapTo[RegionItem]

  def getCountryItem(id: CountryId, lang: Lang): Future[CountryDetailsItem] =
    (countryActor ? CountryReadActor.GetCountryItem(id, lang)).mapTo[CountryDetailsItem]

  def getUser(userId: UserId): Future[User] = userDataActor.ask(GetUser(userId)).mapTo[User]

  def getUserInfoShortItems(imageSize: String)(users: Seq[User]): Seq[UserInfoShort] =
    users.map(user =>
      UserInfoShort(
        id = user.id,
        name = user.name,
        lastName = Some(user.lastName),
        secondName = user.secondName,
        media = user.getMainUserMediaUrl(imageSize)
      )
    )

  def getUsers(ids: Seq[UserId]): Future[Seq[User]] = (userDataActor ? UserDataActor.GetUsers(ids)).mapTo[Seq[User]]

  def getMaritalStatusInfo(id: MaritalStatusId, gender: Gender, lang: Lang): Future[Option[MaritalStatusInfo]] =
    (maritalStatusActor ? MaritalStatusActor.StatusInfoByGender(id, lang, gender)).mapTo[Option[MaritalStatusInfo]]

//  def getMaritalStatusInfo(id: MaritalStatusId, lang: Lang): Future[Option[MaritalStatusInfo]] =
//    getMaritalStatusInfos(Set(id), lang) map (_.headOption)

  def getMaritalStatusInfos(ids: Set[MaritalStatusId], lang: Lang): Future[Seq[MaritalStatusInfo]] =
    (maritalStatusActor ? MaritalStatusActor.Find(ids, lang)).mapTo[Seq[MaritalStatusInfo]]

  def getMaritalStatusInfoMap(seq: Seq[MaritalStatusInfo])(id: MaritalStatusId) = seq.map(x => x.id -> x).toMap.get(id)

  def getPhotoMediaUrls(photoIds: Seq[PhotoId]): Future[Seq[MediaItemDto]] =
    (photoReadActor ? PhotoReadActor.GetMediaItems(photoIds)).mapTo[Seq[MediaItemDto]]
}

package user.response {

import com.tooe.core.domain.MediaUrl
import java.util.Date

case class Author
  (
    id: UserId,
    name: String,
    @JsonProp("lastname") lastName: String,
    media: MediaUrl
    )

  object Author {
    def apply(user: User, imageDimension: String): Author = Author(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      media = user.getMainUserMediaUrl(imageDimension)
    )
  }

  case class ActorItem
  (
    @JsonProperty("id") userId: UserId,
    @JsonProperty("name") name: String,
    @JsonProperty("lastname") lastname: String,
    @JsonProperty("media") media: MediaUrl,
    @JsonProperty("address") address: ActorAddress,
    @JsonProperty("maritalstatus") maritalStatus: Option[MaritalStatusInfo],
    @JsonProperty("birthday") birthday: Option[Date],
    @JsonProperty("gender") gender: Gender
    )

  object ActorItem {
    def apply(maritalStatusMap: MaritalStatusId => Option[MaritalStatusInfo], imageSize: String)(u: User): ActorItem = ActorItem(
      userId = u.id,
      name = u.name,
      lastname = u.lastName,
      media = u.getMainUserMediaUrl(imageSize),
      address = ActorAddress(u.contact.address),
      maritalStatus = u.maritalStatusId flatMap maritalStatusMap,
      birthday = u.birthdayRepr,
      gender = u.gender
    )
  }

  case class ActorShortItem
  (
    @JsonProperty("id") userId: UserId,
    @JsonProperty("name") name: String,
    @JsonProperty("lastname") lastname: String,
    @JsonProperty("media") media: MediaUrl,
    @JsonProperty("address") address: ActorAddress
    )

  object ActorShortItem {
    def apply(maritalStatusMap: MaritalStatusId => Option[MaritalStatusInfo], imageSize: String)(u: User): ActorShortItem = ActorShortItem(
      userId = u.id,
      name = u.name,
      lastname = u.lastName,
      media = u.getMainUserMediaUrl(imageSize),
      address = ActorAddress(u.contact.address)
    )
  }

  case class ActorAddress
  (
    @JsonProperty("country") country: String,
    @JsonProperty("region") region: String
    )

  object ActorAddress {
    def apply(ua: UserAddress): ActorAddress = ActorAddress(
      country = ua.country,
      region = ua.regionName
    )
  }
}

object Implicits {
  implicit class int2Option(val x: Int) extends AnyVal {
    def optionNonZero: Option[Int] = {
      if (x != 0) Some(x)
      else {
        None
      }
    }
  }
}

case class GetUserInfoRequest(userId: UserId, view: Option[ViewTypeEx]) {
  def viewType = view.getOrElse(ViewTypeEx.None)
}
case class GetUsersMainScreenRequest(viewType: Option[ViewTypeEx])
case class GetUsersMainScreenResponse(user: UserMainScreenResponse) extends SuccessfulResponse
trait UserMainScreenResponse

case class UserMainScreenShortItem(
  id: UserId,
  name: String,
  @JsonProperty("lastname")lastName: String,
  @JsonProperty("secondname")secondName: Option[String],
  media: Seq[MediaShortItem],
  address: AddressFullItem,
  currency: CurrencyId,
  statistics: UserMainScreenShortStatisticItem
) extends UserMainScreenResponse
object UserMainScreenShortItem {
  def apply(user: User): UserMainScreenShortItem = UserMainScreenShortItem(
    user.id,
    user.name,
    user.lastName,
    user.secondName,
    user.findAvatarMedia.withDefaultAvatar(Images.User_mainscreen.Short.Media.Main, user).map(MediaShortItem(_, Images.User_mainscreen.Short.Media.Main)),
    currency = user.details.defaultCurrency,
    address = AddressFullItem(user.contact.address),
    statistics = UserMainScreenShortStatisticItem(user.statistics)
  )
}

case class UserMainScreenShortStatisticItem(
  @JsonProperty("presentscount") presentsCount: Option[Int],
  @JsonProperty("newpresentscount") newPresentsCount: Option[Int],
  @JsonProperty("wishescount") wishesCount: Option[Int],
  @JsonProperty("friendscount") friendsCount: Option[Int],
  @JsonProperty("friendrequestcount") friendsRequestCount: Option[Int],
//  @JsonProperty("usereventscount") userEventsCount: Option[Int],
  @JsonProperty("newusereventscount") newUserEventsCount: Option[Int],
  @JsonProperty("favoritelocationscount") favoriteLocationsCount: Option[Int]
)
object UserMainScreenShortStatisticItem {
  def apply(us: UserStatistics): UserMainScreenShortStatisticItem = UserMainScreenShortStatisticItem(
    presentsCount = us.presentsCount.optionNonZero,
    newPresentsCount = us.newStatistic.presentsCount.optionNonZero,
    wishesCount = us.wishesCount.optionNonZero,
    friendsCount = us.friendsCount.optionNonZero,
    friendsRequestCount = us.friendsRequestCount.optionNonZero,
//    userEventsCount = us.eventsCount.optionNonZero,
    newUserEventsCount = us.newStatistic.eventsCount.optionNonZero,
    favoriteLocationsCount = us.favoriteLocationsCount.optionNonZero
  )
}

case class UserMainScreenFullItem(
                               id: UserId,
                               name: String,
                               @JsonProperty("lastname")lastName: String,
                               @JsonProperty("secondname")secondName: Option[String],
                               birthday: Option[Date],
                               gender:Gender,
                               maritalStatus: Option[MaritalStatusInfo],
                               media: Seq[MediaFullItem],
                               address: AddressFullItem,
                               statistics: UserMainScreenStatisticFullItem,
                               @JsonProperty("lastphotos")lastPhotos: Seq[MediaItem],
                               education: Option[String],
                               job: Option[String],
                               aboutMe: Option[String],
                               currency: CurrencyId,
                               star: Option[UserMainScreenFullStarItem]
                               ) extends UserMainScreenResponse
object UserMainScreenFullItem {
  def apply(user: User, maritalStatusOpt: Option[MaritalStatusInfo], starCategories: Seq[StarCategory], lastSixPhotos: Seq[Photo])(implicit lang: Lang): UserMainScreenFullItem = {
    UserMainScreenFullItem(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      secondName = user.secondName,
      birthday = user.birthdayRepr,
      gender = user.gender,
      education = user.details.education,
      job = user.details.job,
      currency = user.details.defaultCurrency,
      star = if (user.isStar) Some(UserMainScreenFullStarItem(categories = starCategories.map(sc =>
        UserMainScreenFullStarCategoryItem(
          categoryId = sc.id,
          name = sc.name.localized.getOrElse("")
        )
      )))
      else None,
      lastPhotos = lastSixPhotos.map(p => MediaItemDto(p.id.id, p.fileUrl).asMediaItem(Images.User_mainscreen.Full.Lastphotos)),
      aboutMe = user.details.aboutMe,
      maritalStatus = maritalStatusOpt,
      media = user.userMedia.withDefaultAvatar(Images.User_mainscreen.Full.Media.Main, user)
        .withDefaultBackground(Images.User_mainscreen.Full.Media.Background).map(um => MediaFullItem(um, user)),
      address = AddressFullItem(user.contact.address),
      statistics = UserMainScreenStatisticFullItem(user.statistics)
    )
  }
}

case class MediaFullItem(id: String,
                         url: String,
                         purpose: Option[String],
                         description: Option[String],
                         desccolor: Option[String],
                         descstyle: Option[String])

object MediaFullItem {
  private def getImageSize(userMedia: UserMedia) =
    userMedia.purpose match {
      case Some("main") => Images.User_mainscreen.Full.Media.Main
      case Some("bg") => Images.User_mainscreen.Full.Media.Background
      case _ => Images.User_mainscreen.Full.Media.Default
    }

  def apply(um: UserMedia, user: User): MediaFullItem = MediaFullItem(
    id = um.url.url.id,
    url = if(um.cdnType == CDNDynamic) um.url.asUrl(getImageSize(um))
    else if(um.purpose == Some("bg")) um.url.asDefaultUrl(getImageSize(um), UserBackgroundDefaultUrlType)
    else um.url.asDefaultUrl(getImageSize(um), UserDefaultUrlType(user.gender)),
    purpose = um.responsePurpose,
    description = um.description,
    desccolor = um.descriptionColor,
    descstyle = um.descriptionStyle)
}

case class UserMainScreenFullStarItem(categories: Seq[UserMainScreenFullStarCategoryItem])
case class UserMainScreenFullStarCategoryItem(categoryId: StarCategoryId, name: String)

case class UserMainScreenStatisticFullItem
(
  @JsonProperty("presentscount") presentsCount: Option[Int],
  @JsonProperty("newpresentscount") newPresentsCount: Option[Int],
  @JsonProperty("wishescount") wishesCount: Option[Int],
  @JsonProperty("friendscount") friendsCount: Option[Int],
  @JsonProperty("friendrequestcount") friendsRequestCount: Option[Int],
//  @JsonProperty("usereventscount") userEventsCount: Int,
  @JsonProperty("newusereventscount") newUserEventsCount: Option[Int],
  @JsonProperty("favoritelocationscount") favoriteLocationsCount: Option[Int],
  @JsonProperty("subscriptionscount") subscriptionsCount: Option[Int],
  @JsonProperty("photoalbumscount") photoAlbumsCount: Option[Int]
)

object UserMainScreenStatisticFullItem {
  def apply(us: UserStatistics): UserMainScreenStatisticFullItem = UserMainScreenStatisticFullItem(
    presentsCount = us.presentsCount.optionNonZero,
    newPresentsCount = us.newStatistic.presentsCount.optionNonZero,
    wishesCount = us.wishesCount.optionNonZero,
    friendsCount = us.friendsCount.optionNonZero,
    friendsRequestCount = us.friendsRequestCount.optionNonZero,
//    userEventsCount = us.eventsCount,
    newUserEventsCount = us.newStatistic.eventsCount.optionNonZero,
    favoriteLocationsCount = us.favoriteLocationsCount.optionNonZero,
    subscriptionsCount = (us.locationSubscriptionsCount + us.starSubscriptionsCount).optionNonZero,
    photoAlbumsCount = us.photoAlbumsCount.optionNonZero
  )
}


case class AddressFullItem(
  country: String,
  region: String,
  @JsonProperty("countryid") countryId: CountryId,
  @JsonProperty("regionid") regionId: RegionId
)
object AddressFullItem {
  def apply(ua: UserAddress): AddressFullItem = AddressFullItem(ua.country, ua.regionName, ua.countryId, ua.regionId)
}

case class UserMainScreenMiniItem(
                               id: UserId,
                               name: String,
                               @JsonProperty("lastname")lastName: String,
                               media: Seq[MediaShortItem],
                               gender: Gender,
                               address: AddressFullItem,
                               @JsonProperty("isstar")isStar: Boolean,
                               currency: CurrencyId
                               ) extends UserMainScreenResponse
object UserMainScreenMiniItem {
  def apply(user: User): UserMainScreenMiniItem = UserMainScreenMiniItem(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    media = user.findAvatarMedia.withDefaultAvatar(Images.User_mainscreen.Mini.Media.Main, user).map(MediaShortItem(_, Images.User_mainscreen.Mini.Media.Main)),
    gender = user.gender,
    address = AddressFullItem(user.contact.address),
    isStar = user.star.isDefined,
    currency = user.details.defaultCurrency
  )
}

object SearchUsersRequestParams {
  val maxLimit = SharedActorSystem.sharedMainActorSystem.settings.config.getInt("constraints.user.search.max-limit")
}

case class SearchUsersRequest(name: Option[String],
                              country: Option[CountryId],
                              region: Option[RegionId],
                              gender: Option[Gender],
                              maritalstatus: Option[String],
                              @JsonProp("entities") responseOptions: Option[Set[UserFields]],
                              offsetLimit: OffsetLimit) extends Validatable {

  val defaultResponseOptions = Set(Users,UsersCount)

  def responseOpts = responseOptions getOrElse defaultResponseOptions

  def usersAreShown:Boolean = responseOpts.contains(Users)

  def usersQuantityIsShown:Boolean = responseOpts.contains(UsersCount)

  def validate(ctx: ValidationContext) {

    if(name.isDefined && name.get.length < 3)
      ctx.fail("Username must contain more that 2 chars")

    if ((responseOpts == Set(UsersCount)) && offsetLimit.offset > 0) {
      ctx.fail(s"Not allowed to have offset > 0 and entities=${UsersCount.id}, because it doesn't return count for offset > 0")
    }

    if (offsetLimit.limit >= SearchUsersRequestParams.maxLimit) {
      ctx.fail(s"Limit should be less than ${SearchUsersRequestParams.maxLimit}")
    }

  }

}

case class SearchUsersResponse(@JsonProperty("userscount") usersCount: Option[Int], users: Seq[SearchUsersItem]) extends SuccessfulResponse

object SearchUsersResponse {
  def apply(users: Seq[User], usersCount: Option[Int]): SearchUsersResponse =
    SearchUsersResponse(usersCount, users map SearchUsersItem.apply)
}

case class SearchUsersItem(
                     id: UserId,
                     name: String,
                     @JsonProperty("lastname") lastName: String,
                     media: MediaUrl,
                     address: AddressShort
                     )

object SearchUsersItem {
  def apply(user: User): SearchUsersItem = SearchUsersItem(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      media = user.getMainUserMediaUrl(Images.Userssearch.Full.User.Media),
      address = Option(user.contact).map(c => AddressShort(c.address.country, c.address.regionName)).getOrElse(null)
    )

  def apply(user: SearchUsersItemDto): SearchUsersItem = SearchUsersItem(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    media = user.mediaUrl,
    address = user.address
  )
}

case class SearchUsersItemDto(
                            id: UserId,
                            name: String,
                            @JsonProperty("lastname") lastName: String,
                            mediaUrl: MediaUrl,
                            address: AddressShort
                            )

object SearchUsersItemDto {
  def apply(imageSize: String)(user: User): SearchUsersItemDto = SearchUsersItemDto(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    mediaUrl = user.getMainUserMediaUrl(imageSize),
    address = Option(user.contact).map(c => AddressShort(c.address.country, c.address.regionName)).getOrElse(null)
  )
}

case class SearchStarsResponse(@JsonProperty("starscount") starsCount: Long, stars: Seq[SearchStarsItem]) extends SuccessfulResponse

case class SearchStarsItem( id: UserId,
                            name: String,
                            @JsonProperty("lastname") lastName: String,
                            media: MediaUrl,
                            address: AddressShort,
                            @JsonProperty("subscriberscount")  subscribersCount: Long,
                            categories: Seq[StarCategoryResponse])

object SearchStarsItem {
  def apply(user: User, starCategories: Map[StarCategoryId, Option[String]]): SearchStarsItem = SearchStarsItem(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    media = user.getMainUserMediaUrl(""),
    address = Option(user.contact).map(c => AddressShort(c.address.country, c.address.regionName)).getOrElse(null),
    subscribersCount = 0, //TODO #2223
    categories = user.star.map(_.starCategoryIds.map(c => StarCategoryResponse(c, starCategories(c).getOrElse("")))).getOrElse(Nil)
  )

}

case class SubscriberStarsItem(id: UserId,
                            name: String,
                            @JsonProperty("lastname") lastName: String,
                            @JsonProperty("secondname") secondName: Option[String],
                            media: MediaUrl,
                            address: AddressShort)

object SubscriberStarsItem {
  def apply(user: User): SubscriberStarsItem = SubscriberStarsItem(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    secondName = user.secondName,
    media = user.getMainUserMediaUrl(""),
    address = Option(user.contact).map(c => AddressShort(c.address.country, c.address.regionName)).getOrElse(null)
  )

  def apply(user: SubscriberStarsItemDto): SubscriberStarsItem = SubscriberStarsItem(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    secondName = user.secondName,
    media = user.mediaUrl,
    address = user.address
  )

}

case class SubscriberStarsItemDto(id: UserId,
                               name: String,
                               @JsonProperty("lastname") lastName: String,
                               @JsonProperty("secondname") secondName: Option[String],
                               mediaUrl: MediaUrl,
                               address: AddressShort)

object SubscriberStarsItemDto {
  def apply(user: User): SubscriberStarsItemDto = SubscriberStarsItemDto(
    id = user.id,
    name = user.name,
    lastName = user.lastName,
    secondName = user.secondName,
    mediaUrl = user.getMainUserMediaUrl(Images.Locationsubscribers.Full.User.Media),
    address = Option(user.contact).map(c => AddressShort(c.address.country, c.address.regionName)).getOrElse(null)
  )

}

object SearchStarsResponse {
  def apply(stars: Seq[User], starsCount: Long, starCategories: Map[StarCategoryId, Option[String]]): SearchStarsResponse =
    SearchStarsResponse(starsCount, stars map(SearchStarsItem(_, starCategories)))
}

case class UserInfoResponse(user: UserInfoResponseContent) extends SuccessfulResponse
trait UserInfoResponseContent

case class UserInfoShortResponseContent(
  id: UserId,
  name: String,
  @JsonProp("lastname") lastName: String,
  secondName: Option[String],
  media: Seq[MediaShortItem],
  @JsonProp("lastphotos")lastPhotos: Seq[MediaItem],
  birthday: Option[Date],
  gender: Gender,
  @JsonProp("maritalstatus") maritalStatus: Option[MaritalStatusInfo],
  address: AddressFullItem,
  currency: CurrencyId,
  @JsonProp("isstar")isStar: Option[Boolean],
  @JsonProp("isfriend")isFriend: Option[Boolean],
  @JsonProp("friendshipstatus") friendshipStatus: Option[FriendshipStatus],
  @JsonProp("usergroups") userGroups: Option[Seq[FriendshipType]]
) extends UserInfoResponseContent

object UserInfoShortResponseContent {
  def apply(lastSixPhotos: Seq[MediaItemDto],
            maritalStatusInfoOpt: Option[MaritalStatusInfo],
            isFriends: Boolean,
            friendshipStatus: Option[FriendshipStatus],
            friendshipGroups: Option[Seq[FriendshipType]]
             )(user: User): UserInfoShortResponseContent =
    UserInfoShortResponseContent(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      secondName = user.secondName,
      media = user.findAvatarMedia.withDefaultAvatar(Images.User.Short.Self.Media, user).map(m => MediaShortItem(m, Images.User.Short.Self.Media)),
      lastPhotos = lastSixPhotos.map(_.asMediaItem(Images.User.Short.Lastphotos.Media)),
      birthday = user.birthdayRepr,
      gender = user.gender,
      address = AddressFullItem(user.contact.address),
      maritalStatus = maritalStatusInfoOpt,
      currency = user.details.defaultCurrency,
      isStar = if(user.isStar) Some(true) else None,
      isFriend = if(isFriends) Some(true) else None,
      friendshipStatus = friendshipStatus,
      userGroups = friendshipGroups
    )
}

case class UserInfoFullResponseContent(
                         id: UserId,
                         name: String,
                         @JsonProp ("lastname") lastName: String,
                         secondName: Option[String],
                         media: Seq[UserMediaResponseItem],
                         @JsonProp("lastphotos")lastPhotos: Seq[MediaItem],
                         birthday: Option[Date],
                         gender: Gender,
                         @JsonProp("maritalstatus") maritalStatus: Option[MaritalStatusInfo],
                         address: AddressFullItem,
                         education: Option[String],
                         job: Option[String],
                         @JsonProp("aboutme") aboutMe: Option[String],
                         currency: CurrencyId,
                         @JsonProp("isstar")isStar: Option[Boolean],
                         @JsonProp("isfriend")isFriend: Option[Boolean],
                         @JsonProp("friendshipstatus") friendshipStatus: Option[FriendshipStatus],
                         @JsonProp("usergroups") userGroups: Option[Seq[FriendshipType]],
                         statistics: UserInfoFullResponseStatistic
                         ) extends UserInfoResponseContent

object UserInfoFullResponseContent {

  def apply(lastSixPhotos: Seq[MediaItemDto],
            maritalStatusInfoOpt: Option[MaritalStatusInfo],
            isFriends: Boolean,
            friendshipStatusOpt: Option[FriendshipStatus],
            friendshipGroups: Option[Seq[FriendshipType]]
             )(user: User): UserInfoFullResponseContent =
    UserInfoFullResponseContent(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      secondName = user.secondName,
      media = user.userPhotoMedia.withDefaultAvatar(Images.User.Full.Self.Media, user)
        .withDefaultBackground(Images.User.Full.Self.Background)
        .map(UserMediaResponseItem(_, user)),
      lastPhotos = lastSixPhotos.map(_.asMediaItem(Images.User.Full.Lastphotos.Media)),
      birthday = user.birthdayRepr,
      gender = user.gender,
      maritalStatus = maritalStatusInfoOpt,
      address = AddressFullItem(user.contact.address),
      isFriend = if (isFriends) Option(isFriends) else None,
      education = user.details.education,
      job = user.details.job,
      aboutMe = user.details.aboutMe,
      currency = user.details.defaultCurrency,
      isStar = if(user.isStar) Some(true) else None,
      friendshipStatus = friendshipStatusOpt,
      userGroups = friendshipGroups,
      statistics = UserInfoFullResponseStatistic(user.statistics)
    )
}

case class UserInfoFullResponseStatistic(
  @JsonProp("friendscount")friendsCount: Option[Int],
  @JsonProp("wishescount")wishesCount: Option[Int],
  @JsonProp("photoalbumscount")photoalbumsCount: Option[Int],
  @JsonProp("favoritelocationscount")favoriteLocationsCount: Option[Int]
 )

object UserInfoFullResponseStatistic {
  def apply(us: UserStatistics): UserInfoFullResponseStatistic = UserInfoFullResponseStatistic(
    friendsCount = us.friendsCount.optionNonZero,
    wishesCount = us.wishesCount.optionNonZero,
    photoalbumsCount = us.photoAlbumsCount.optionNonZero,
    favoriteLocationsCount = us.favoriteLocationsCount.optionNonZero
  )
}

case class UserInfoMiniResponseContent(id: UserId,
                                name: String,
                                @JsonProp("lastname") lastName: String,
                                @JsonProp("mainmedia") mainMedia: MediaUrl) extends UserInfoResponseContent

object UserInfoMiniResponseContent {
  def apply(user: User): UserInfoMiniResponseContent =
    UserInfoMiniResponseContent(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      mainMedia = user.getMainUserMediaUrl(Images.User.Mini.Self.Media)
    )
}

case class GetUserMoreInfoResponse(user: UserMoreInfoItem) extends SuccessfulResponse

object GetUserMoreInfoResponse {
  def apply(user: User): GetUserMoreInfoResponse = GetUserMoreInfoResponse(UserMoreInfoItem(user))
}

case class UserMoreInfoItem(id: UserId, details: UserMoreInfoDetailsItem)

object UserMoreInfoItem {
  def apply(user: User): UserMoreInfoItem = UserMoreInfoItem(
    user.id,
    UserMoreInfoDetailsItem(
      user.details.education.getOrElse(""),// TODO education, job and aboutMe fields will be optional in v0.2 of REST spec
      user.details.job.getOrElse(""),
      user.details.aboutMe.getOrElse("")
    )
  )
}

case class UserMoreInfoDetailsItem(education: String, job: String, @JsonProp("aboutme") aboutMe: String)

case class UserProfileShortResponse(@JsonProp("userprofile") userProfile: UserProfileShortItem) extends SuccessfulResponse

case class UserProfileShortItem(name: String,
                       @JsonProp("lastname") lastName: String,
                       email: String,
                       @JsonProp("mainphone") mainPhone: Option[PhoneShort],
                       gender: Gender,
                       @JsonProp("maritalstatus") maritalStatus: Option[MaritalStatusInfo],
                       birthday: Option[Date],
                       media: MediaUrl,
                       region: RegionItem,
                       country: CountryDetailsItem)

object UserProfileShortItem{

  def apply(region: RegionItem, country: CountryDetailsItem, maritalStatusInfo: Option[MaritalStatusInfo])(user: User): UserProfileShortItem = {
    val userContact = user.contact
    val userPhone = userContact.phones.main
    UserProfileShortItem(
      name = user.name,
      lastName = user.lastName,
      email = user.contact.email,
      mainPhone = userPhone.map(p => PhoneShort(p)),
      gender = user.gender,
      maritalStatus = maritalStatusInfo,
      birthday = user.birthdayRepr,
      media = user.getMainUserMediaUrl(Images.User_profile.Short.Media.Main),
      region = region,
      country = country
    )
  }
}

case class UserProfileFullResponse(@JsonProp("userprofile") userProfile: UserProfileFullItem) extends SuccessfulResponse

case class UserProfileFullItem(name: String,
                           @JsonProp("lastname") lastName: String,
                           gender: Gender,
                           @JsonProp("maritalstatus") maritalStatus: Option[MaritalStatusInfo],
                           birthday: Option[Date],  // TODO should be in seconds
                           @JsonProp("country")countryItem: CountryDetailsItem,
                           @JsonProp("region")regionItem: RegionItem,
                           email: String,
                           @JsonProp("mainphone") mainPhone: Option[PhoneShort],
                           phones:Option[Seq[PhoneShort]],
                           education: Option[String],
                           job: Option[String],
                           @JsonProp("aboutme") aboutMe: Option[String],
                           settings: UserSettingsProfileItem,
                           @JsonProp("messagesettings")messageSettings: UserMessageSettingProfileItem,
                           @JsonProp("usermedia") userMedia: Seq[UserMediaProfileItem])

object UserProfileFullItem {
  def apply(region: RegionItem, country: CountryDetailsItem, maritalStatusInfo: Option[MaritalStatusInfo])(user: User): UserProfileFullItem = {
    val userPhones = user.contact.phones
    UserProfileFullItem(
      name = user.name,
      lastName = user.lastName,
      birthday = user.birthdayRepr,
      regionItem = region,
      countryItem = country,
      email = user.contact.email,
      gender = user.gender,
      maritalStatus = maritalStatusInfo,
      mainPhone = userPhones.mainShort,
      phones = userPhones.additionalShortOpt,
      education  = user.details.education,
      job  = user.details.job,
      aboutMe  = user.details.aboutMe,
      settings = UserSettingsProfileItem(user.settings),
      messageSettings = UserMessageSettingProfileItem(user.settings.messageSettings),
      userMedia = user.userMedia.withDefaultAvatar(Images.User_profile.Full.Media.Main, user).map(p => UserMediaProfileItem(p, user))
    )
  }
}

case class UserSettingsProfileItem(@JsonProp("pagerights")pageRights: Seq[String],
                                   @JsonProp("maprights")mapRights: Seq[String])

object UserSettingsProfileItem {
  def apply(us: UserSettings): UserSettingsProfileItem = UserSettingsProfileItem(
    pageRights = us.pageRights,
    mapRights = us.mapRights
  )
}

case class UserMessageSettingProfileItem(@JsonProp("showtext")showText: Option[Boolean],
                                         @JsonProp("playaudio")playAudio: Option[Boolean],
                                         @JsonProp("sendemail")sendEmail: Option[UserSendEmailEvent])

object UserMessageSettingProfileItem {
  def apply(ums: UserMessageSetting): UserMessageSettingProfileItem = UserMessageSettingProfileItem(
    showText = ums.showMessageText,
    playAudio = ums.soundsEnabled,
    sendEmail = ums.sendEmailEvent
  )
}

case class UserMediaProfileItem(id: String,
                                url: String,
                                purpose: Option[String] = None,
                                description: Option[String] = None,
                                style: Option[String] = None,
                                color: Option[String] = None
                                 )
object UserMediaProfileItem {

  def getImageSize(um: UserMedia) = um.purpose match {
    case Some("main") => Images.User_profile.Full.Media.Main
    case Some("bg") => Images.User_profile.Full.Media.Background
    case _ => Images.User_profile.Full.Media.Default
  }

  def apply(um: UserMedia, user: User): UserMediaProfileItem = UserMediaProfileItem(
    id = um.url.url.id,
    url = if(um.cdnType == CDNDynamic) um.url.asUrl(getImageSize(um))
    else if(um.purpose == Some("bg")) um.url.asDefaultUrl(getImageSize(um), UserBackgroundDefaultUrlType)
    else um.url.asDefaultUrl(getImageSize(um), UserDefaultUrlType(user.gender)),
    purpose = um.responsePurpose,
    description = um.description,
    style = um.descriptionStyle,
    color = um.descriptionColor
  )


}

case class GetStarResponse(star: StarDetailsItem) extends SuccessfulResponse

case class GetStarMiniResponse(star: StarMiniDetailsItem) extends SuccessfulResponse

case class StarDetailsItem(id: UserId,
                       name: String,
                       @JsonProp("lastname") lastName: String,
                       @JsonProp("secondname") secondName: Option[String],
                       birthday: Option[Date],
                       gender: Gender,
                       address: AddressShort,
                       @JsonProp("selfsubscribed") selfSubscribed: Boolean,
                       @JsonProp("lastphotos") lastPhotos: Seq[MediaItem],
                       media: Seq[MediaDetails])

object StarDetailsItem {
  def apply(selfSubscribed: Boolean, lastPhotos: Seq[MediaItemDto], media: Seq[UserMedia])(star: User): StarDetailsItem = StarDetailsItem(
    id = star.id,
    name = star.name,
    lastName = star.lastName,
    secondName = star.secondName,
    birthday = star.birthdayRepr,
    gender = star.gender,
    address = AddressShort(star.contact.address.country, star.contact.address.regionName),
    selfSubscribed = selfSubscribed,
    lastPhotos = lastPhotos.map(MediaItem(_, Images.Star.Full.Self.Media)),
    media = media.map(um => MediaDetails(um.url.asUrl(Images.Star.Full.Self.Media), um.responsePurpose))
  )
}

case class StarMiniDetailsItem(id: UserId,
                           name: String,
                           @JsonProp("lastname") lastName: String,
                           @JsonProp("mainmedia") media: MediaUrl)

object StarMiniDetailsItem {
  def apply(media: Seq[UserMedia])(star: User): StarMiniDetailsItem = StarMiniDetailsItem(
    id = star.id,
    name = star.name,
    lastName = star.lastName,
    media = media.headOption.map(_.url).asMediaUrl("", UserDefaultUrlType(star.gender))
  )
}
case class MediaDetails(@JsonProperty("url") imageUrl: String, purpose: Option[String])

case class AuthorDetails(id: UserId,
                         name: String,
                         @JsonProperty("lastname") lastName: String,
                         media: MediaUrl)
object AuthorDetails {
  def apply(user: User, imageDimension: String): AuthorDetails =
    AuthorDetails(user.id,
      user.name,
      user.lastName,
      user.getMainUserMediaUrl(imageDimension))
}

case class LikeDetails(author: AuthorDetails)

object LikeDetails {
  def apply(authorDetailsMap: Map[UserId, AuthorDetails])(userId: UserId): LikeDetails =
    LikeDetails(authorDetailsMap(userId))
}

case class UserMediaResponseItem(
  url: String,
  purpose: Option[String] = None,
  description: Option[String] = None,
  @JsonProperty("desccolor") descriptionColor: Option[String] = None,
  @JsonProperty("descstyle") descriptionStyle: Option[String] = None
)

object UserMediaResponseItem {

  private def getImageSize(userMedia: UserMedia): String = userMedia.purpose match {
    case Some("main") => Images.User.Full.Self.Media
    case Some("bg") => Images.User.Full.Self.Background
    case _ => Images.User.Full.Self.Default
  }

  def apply(um: UserMedia, user: User): UserMediaResponseItem =
    UserMediaResponseItem(url = if(um.cdnType == CDNDynamic) um.url.asUrl(getImageSize(um))
      else if(um.purpose == Some("bg")) um.url.asDefaultUrl(getImageSize(um), UserBackgroundDefaultUrlType)
      else um.url.asDefaultUrl(getImageSize(um), UserDefaultUrlType(user.gender)),
      description = um.description,
      purpose = um.responsePurpose,
      descriptionColor = um.descriptionColor,
      descriptionStyle = um.descriptionStyle)
}

case class CheckedInUser
(
  id: UserId,
  name: String,
  lastName: String,
  secondName: Option[String],
  media: MediaUrl
  )

object CheckedInUser {
  def apply(u: User, imageSize: String): CheckedInUser = CheckedInUser(
    id = u.id,
    name = u.name,
    lastName = u.lastName,
    secondName = u.secondName,
    media = u.getMainUserMediaUrl(imageSize)
  )
}

case class LocationCheckinInfoItem
(
  @JsonProperty("users_count") usersCount: Int = 0,
  @JsonProperty("boys_count") boysCount: Int = 0,
  @JsonProperty("girls_count") girlsCount: Int = 0,
  @JsonProperty("friends_count") friendsCount: Int = 0,
  users: Seq[UserInfoShort] = Nil,
  friends: Seq[UserInfoShort] = Nil
  )

object LocationCheckinInfoItem {
  def apply
  (
    checkedInFriends: Seq[User],
    checkedInOthers: Seq[User]
    ): LocationCheckinInfoItem
  = {
    val allCheckedInUsers = checkedInFriends ++ checkedInOthers
    LocationCheckinInfoItem(
      usersCount = allCheckedInUsers.size,
      boysCount = allCheckedInUsers.count(_.gender == Gender.Male),
      girlsCount = allCheckedInUsers.count(_.gender == Gender.Female),
      friendsCount = checkedInFriends.size,
      users = allCheckedInUsers map UserInfoShort(Images.Location.Short.Users.Media),
      friends = checkedInFriends map UserInfoShort(Images.Location.Short.Friends.Media)
    )
  }
}

case class GetOtherUserStatisticsRequest
(
  userId: UserId,
  statistics: Option[Set[UserStatisticViewType]]
  ) {
  def statisticsView = statistics.getOrElse(Set(UserStatisticViewType.Full))
}
case class GetOtherUserStatisticResponse( statistics: GetOtherUserStatisticsResponseContent) extends SuccessfulResponse

case class GetOtherUserStatisticsResponseContent
(
  @JsonProperty("friendscount") friendsCount: Option[Int] = None,
  @JsonProperty("friendsonlinecount") friendsOnlineCount: Option[Int] = None,
  @JsonProperty("wishescount") wishesCount: Option[Int] = None,
  @JsonProperty("photoalbumscount") photoAlbumsCount: Option[Int] = None,
  @JsonProperty("favoritelocationscount") favoriteLocationsCount: Option[Int] = None
)

case class GetUsersOwnStatisticsRequest
(
  statistics: Option[Set[UsersOwnStatisticsViewType]]
) {
  lazy val statisticsView = statistics.map(_.toSeq).getOrElse(UsersOwnStatisticsViewType.valuesWithoutFriendsOnline)
}

case class GetUsersOwnStatisticsResponse( statistics: GetUsersOwnStatisticsResponseItem) extends SuccessfulResponse

case class GetUsersOwnStatisticsResponseItem
(
  @JsonProperty("presentscount") presentsCount: Option[Int] = None,
  @JsonProperty("presentstypecount") presentsTypeCount: Option[Int] = None,
  @JsonProperty("certificatestypecount") certificatesTypeCount: Option[Int] = None,
  @JsonProperty("sentpresentscount") sentPresentsCount: Option[Int] = None,
  @JsonProperty("sentpresentstypecount") sentPresentsTypeCount: Option[Int] = None,
  @JsonProperty("sentcertificatestypecount") sentCertificatesTypeCount: Option[Int] = None,
  @JsonProperty("friendscount") friendsCount: Option[Int] = None,
  @JsonProperty("friendsonlinecount") friendsOnlineCount: Option[Int] = None,
  @JsonProperty("friendrequestcount") friendRequestCount: Option[Int] = None,
  @JsonProperty("wishescount") wishesCount: Option[Int] = None,
  @JsonProperty("ffwishescount") fulfilledWishesCount: Option[Int] = None,
  @JsonProperty("subscriptionscount") subscriptionsCount: Option[Int] = None,
  @JsonProperty("starsubscriptionscount") starSubscriptionsCount: Option[Int] = None,
  @JsonProperty("locsubscriptionscount") locationSubscriptionsCount: Option[Int] = None,
  @JsonProperty("photoalbumscount") photoalbumsCount: Option[Int] = None,
  @JsonProperty("newpresentscount") newPresentsCount: Option[Int] = None,
  @JsonProperty("newusereventscount") newUserEventsCount: Option[Int] = None,
  @JsonProperty("favoritelocationscount") favoriteLocationsCount: Option[Int] = None
  )

object GetUsersOwnStatisticsResponseItem {
  import UsersOwnStatisticsViewType._
  def apply(user: User, friendsOnlineCount: Int)(implicit fields: Seq[UsersOwnStatisticsViewType]): GetUsersOwnStatisticsResponseItem = {
    def projection[T](field: UsersOwnStatisticsViewType)(result: => Option[T]) = fields.find(_ == field).flatMap(_ => result)
    GetUsersOwnStatisticsResponseItem(
      presentsCount = projection(Presents)((user.statistics.presentsCount + user.statistics.certificatesCount).optionNonZero),
      presentsTypeCount = projection(PresentsType)(user.statistics.presentsCount.optionNonZero),
      certificatesTypeCount = projection(CertificatesType)(user.statistics.certificatesCount.optionNonZero),
      sentPresentsCount = projection(SentPresents)((user.statistics.sentPresentsCount + user.statistics.sentCertificatesCount).optionNonZero),
      sentPresentsTypeCount = projection(SentPresentsType)(user.statistics.sentPresentsCount.optionNonZero),
      sentCertificatesTypeCount = projection(SentCertificatesType)(user.statistics.sentCertificatesCount.optionNonZero),
      friendsCount = projection(Friends)(user.statistics.friendsCount.optionNonZero),
      friendsOnlineCount = projection(FriendsOnline)(friendsOnlineCount.optionNonZero),
      friendRequestCount = projection(FriendsRequests)(user.statistics.friendsRequestCount.optionNonZero),
      wishesCount = projection(Wishes)(user.statistics.wishesCount.optionNonZero),
      fulfilledWishesCount = projection(FulfilledWishes)(user.statistics.fulfilledWishesCount.optionNonZero),
      subscriptionsCount = projection(Subscriptions)((user.statistics.starSubscriptionsCount + user.statistics.locationSubscriptionsCount).optionNonZero),
      starSubscriptionsCount = projection(StarsSubscriptions)(user.statistics.starSubscriptionsCount.optionNonZero),
      locationSubscriptionsCount = projection(LocationsSubscriptions)(user.statistics.locationSubscriptionsCount.optionNonZero),
      photoalbumsCount = projection(PhotoAlbums)(user.statistics.photoAlbumsCount.optionNonZero),
      newPresentsCount = projection(NewPresents)(user.statistics.newStatistic.presentsCount.optionNonZero),
      newUserEventsCount = projection(NewEvents)(user.statistics.newStatistic.eventsCount.optionNonZero),
      favoriteLocationsCount = projection(Favorites)(user.statistics.favoriteLocationsCount.optionNonZero)
    )
  }
}

case class GetUsersOnlineStatusesRequest(userIds: Option[Set[UserId]]) extends Validatable {
  def validate(ctx: ValidationContext): Unit = {
    if (userIds.exists(_.size > 20)) ctx.fail("Too many user ids")
  }
}
case class GetUsersOnlineStatusesResponse(users: Seq[GetUsersOnlineStatusesResponseItem]) extends SuccessfulResponse
case class GetUsersOnlineStatusesResponseItem
(
  id: UserId,
  name: String,
  @JsonProperty("lastname")lastName: String,
  media: MediaUrl,
  @JsonProperty("onlinestatus")onlineStatus: OnlineStatusResponseItem)

object GetUsersOnlineStatusesResponseItem {
  def apply(user: User, onlineStatuses: OnlineStatusResponseItem): GetUsersOnlineStatusesResponseItem = {
    GetUsersOnlineStatusesResponseItem(
      id = user.id,
      name = user.name,
      lastName = user.lastName,
      media = user.getMainUserMediaUrl(Images.Users_online.Full.User.Media),
      onlineStatus = onlineStatuses
    )
  }
}

case class OnlineStatusResponseItem( id: OnlineStatusId, name: String)

object OnlineStatusResponseItem {
  def apply(os: OnlineStatus)(implicit lang: Lang): OnlineStatusResponseItem =
    OnlineStatusResponseItem(os.id, os.name.localized.getOrElse(""))
}

case class GetMutualFriendsResponse
(
  @JsonProperty("mutualfriendscount") mutualFriendsCount: Int,
  @JsonProperty("mutualfriends") mutualFriends: Seq[MutualFriendResponseItem]
) extends SuccessfulResponse

case class MutualFriendResponseItem
(
  @JsonProperty("userid") userId: UserId,
  name: String,
  @JsonProperty("lastname") lastName: String,
  media: MediaUrl
)

object MutualFriendResponseItem {
  def apply(user: User): MutualFriendResponseItem = MutualFriendResponseItem(
    userId = user.id,
    name = user.name,
    lastName = user.lastName,
    media = user.getMainUserMediaUrl(Images.Mutualfriends.Full.User.Media)
  )
}

case class PaymentUser(@JsonProp("actorid") id: UserId,
                         name: String,
                         @JsonProp("lastname") lastName: String,
                         email: String,
                         @JsonProp("phone") mainPhone: Option[String],
                         star: Option[Boolean],
                         @JsonProp("staragentid") staragentId: Option[AdminUserId])

object PaymentUser {

  def apply(user: User): PaymentUser = {
    PaymentUser(id = user.id,
      name = user.name,
      lastName = user.lastName,
      email = user.contact.email,
      mainPhone = user.contact.phones.main.map(_.fullNumber),
      star = if(user.star.isDefined) Some(true) else None,
      staragentId = user.star.flatMap(_.agentId)
    )
  }

}

case class StarItem(
  @JsonProp("id") id: UserId,
  @JsonProp("name") name: String,
  @JsonProp("lastname") lastName: Option[String],
  @JsonProp("media") media: MediaUrl,
  categories: Seq[StarCategoryId]
)

object StarItem {

  def apply(user: User): StarItem = StarItem(
    id = user.id,
    name =  user.name,
    lastName = Some(user.lastName),
    media = user.getMainUserMediaUrl(Images.Star.Full.Self.Media),
    categories = user.star.map(_.starCategoryIds).getOrElse(Nil)
  )

}

case class StarStatisticsResponse(statistics: StarStatistics) extends SuccessfulResponse

case class StarStatistics(
  @JsonProp("wishescount") wishesCount: Option[Int],
  @JsonProp("photoalbumscount") photoAlbumsCount: Option[Int],
  @JsonProp("favoritelocationscount") favoriteLocationsCount: Option[Int],
  @JsonProp("subscriberscount") subscribersCount: Option[Int]
)

object StarStatisticsResponse {

  private def getStatisticValue(fields: Seq[StarStatisticField], statistic: StarStatisticField, value: => Option[Int]): Option[Int] =
    if(fields.contains(statistic)) value else None

  def apply(user: User, fields: Seq[StarStatisticField]): StarStatisticsResponse =
    StarStatisticsResponse(
      StarStatistics(
        wishesCount = getStatisticValue(fields, StarStatisticField.WishesCount, Some(user.statistics.wishesCount)),
        photoAlbumsCount = getStatisticValue(fields, StarStatisticField.PhotoAlbumsCount, Some(user.statistics.photoAlbumsCount)),
        favoriteLocationsCount = getStatisticValue(fields, StarStatisticField.FavoriteLocationsCount, Some(user.statistics.favoriteLocationsCount)),
        subscribersCount = getStatisticValue(fields, StarStatisticField.SubscribersCount, user.star.map(_.subscribersCount))
      )
    )
}

case class StarSubscriptionItem(
  @JsonProp("id") id: UserId,
  @JsonProp("name") name: String,
  @JsonProp("lastname") lastName: Option[String],
  @JsonProp("media") media: MediaUrl,
  @JsonProp("subscriberscount") subscribersCount: Int
)

object StarSubscriptionItem {

  def apply(user: User): StarSubscriptionItem =
    StarSubscriptionItem(
      id = user.id,
      name =  user.name,
      lastName = Some(user.lastName),
      media = user.getMainUserMediaUrl(Images.Friends.Full.User.Media),
      subscribersCount = user.star.map(_.subscribersCount).getOrElse(0)
    )

}

case class TopStarsSearchResponse(stars: Seq[StarSubscriptionItem]) extends SuccessfulResponse