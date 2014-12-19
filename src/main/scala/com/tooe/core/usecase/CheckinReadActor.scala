package com.tooe.core.usecase

import com.tooe.api.JsonProp
import com.tooe.core.application.Actors
import com.tooe.api.service.{ExecutionContextProvider, OffsetLimit, SuccessfulResponse}
import com.tooe.core.db.mongo.domain
import concurrent.Future
import com.tooe.core.domain._
import java.util.Date
import com.tooe.core.usecase.checkin.{CheckinSearchSortType, CheckinDataActor}
import com.tooe.core.usecase.checkin.CheckinDataActor._
import com.tooe.api.validation.{ValidationContext, Validatable}
import com.tooe.core.usecase.checkin.CheckinDataActor.FindAllCheckedInUsers
import com.tooe.core.domain.CheckinId
import com.tooe.core.usecase.checkin.CheckinDataActor.FindCheckins
import com.tooe.core.service.SearchNearParams
import com.tooe.core.db.mongo.domain.CheckinBaseProjection
import com.tooe.core.usecase.checkin.CheckinDataActor.CountAllCheckedInUsers
import com.tooe.core.domain.LocationId
import com.tooe.core.usecase.checkin.CheckinDataActor.GetOnlyCheckedInUsers
import com.tooe.core.service.CheckinWithDistance
import com.tooe.core.domain.UserId
import com.tooe.core.domain.MediaUrl
import com.tooe.core.util.Images
import com.tooe.core.util.MediaHelper._

object CheckinReadActor {
  final val Id = Actors.CheckinRead

  case class GetUsersCheckinsByLocation(request: UsersCheckinRequest, currentUserId: UserId)
  case class GetCheckinByUserId(userId: UserId)
  case class GetCheckinsStatistic(locationIds: Seq[LocationId], currentUserId: UserId)

  case class SearchNearRequest(params: SearchNearParams, sort: Option[CheckinSearchSortType]) {
    def getSort = sort getOrElse CheckinSearchSortType.Distance
  }

  case class GetLocationCheckinInfoItem(id: LocationId, currentUserId: UserId)
}

class CheckinReadActor extends AppActor with ExecutionContextProvider with FriendReadComponent {

  lazy val checkinDataActor = lookup(CheckinDataActor.Id)
  lazy val userReadActor = lookup(UserReadActor.Id)

  import com.tooe.core.usecase.CheckinReadActor._

  def receive = {
    case GetLocationCheckinInfoItem(locationId, currentUserId) =>
      getAllCheckedInUsers(locationId, currentUserId, OffsetLimit()) flatMap getLocationCheckedInUsers(currentUserId) pipeTo sender

    case GetUsersCheckinsByLocation(request, currentUserId) => {

      val locationId = request.locationId

      def usersCheckinResponse = {
        def usersQtyFtr = if (request.includesUsersQty)
                            countAllCheckedInUsers(locationId, currentUserId).map(Some(_))
                          else
                            Future(None)
        def usersFtr = if (request.includesUsersShort)
                          getAllCheckedInUsers(locationId, currentUserId, request.offsetLimit)
                       else
                          Future(Nil)
        for {
          usersQty <- usersQtyFtr
          checkedInUserIds <- usersFtr
          checkedInUsers <- getCheckedInUserItems(checkedInUserIds, Images.Locationcheckins.Short.Users.Media)
        } yield CheckinUsersResponse(usersQty, checkedInUsers)
      }

      def friendsCheckinResponse = {
        def friendsQtyFtr = if (request.includesFriendsQty)
                              countCheckedInFriends(locationId, currentUserId).map(Some(_))
                            else
                              Future(None)
        def friendsFtr = if (request.includesFriendsShort)
                            getCheckedInFriends(locationId, currentUserId, request.offsetLimit)
                         else
                            Future(Nil)
        for {
          friendsQty <- friendsQtyFtr
          checkedInFriendsIds <- friendsFtr
          checkedInFriends   <- getCheckedInUserItems(checkedInFriendsIds, Images.Locationcheckins.Short.Friends.Media)
        } yield CheckinFriendsResponse(friendsQty, checkedInFriends)
      }

      if (request.isRelatedToUsers)
        usersCheckinResponse
      else
        friendsCheckinResponse
    } pipeTo sender

    case GetCheckinByUserId(userId) => checkinDataActor.ask(CheckinDataActor.GetCheckinByUserId(userId)).mapTo[domain.Checkin]
      .map(c => UsersCheckinSearchResponse(
      UsersCheckinSearchLocation(
        UsersCheckinSearchLocationItem(
          id = c.location.locationId,
          name = c.location.name,
          openingHours = c.location.openingHours,
          coordinates = c.location.coordinates,
          address = c.location.address,
          media = c.location.media.asMediaUrl(Images.Checkin.Full.Location.Media, LocationDefaultUrlType)
        )
      )
    )) pipeTo sender

    case request: SearchNearRequest =>
      val checkinsFtr = request.getSort match {
        case CheckinSearchSortType.Distance => checkinsOrderedByDistance(request.params)
        case CheckinSearchSortType.Name     => checkinsOrderedByName(request.params)
      }
      val future = for {
        (checkins, qty) <- checkinsFtr zip checkinsQty(request.params)
        friends <- getUserFriends(request.params.userId)
        items  = checkins map CheckinSearchItem(friends)
      } yield CheckinsSearchResponse(qty, items)
      future pipeTo sender

    case GetCheckinsStatistic(locationIds, currentUserId) =>
      checkinDataActor.ask(FindCheckins(locationIds)).mapTo[Seq[domain.Checkin]].flatMap(checkins => {
        val locationToCheckins = checkins.groupBy(_.location.locationId).map( locationToCheckins => {
            for {
            friends <- checkinDataActor.ask(GetOnlyCheckedInUsers(locationToCheckins._2.map(_.user.userId), locationToCheckins._1)).mapTo[Seq[UserId]]
            } yield Map(locationToCheckins._1 -> CheckinsStatistic(
              allCount = locationToCheckins._2.size,
              //TODO should compare genders instead of it ids
              boysCount = locationToCheckins._2.count(_.user.gender.id == "m"),
              girlsCount = locationToCheckins._2.count(_.user.gender.id == "f"),
              friendsCount = friends.size
            ))
        })
        Future.sequence( locationToCheckins)
      }).map(_.reduceOption((res, next) => res ++ next).getOrElse(Map())) pipeTo sender
  }


  def checkinsOrderedByName(request: SearchNearParams): Future[Seq[CheckinWithDistance]] =
    (checkinDataActor ? CheckinDataActor.SearchNearOrderedByName(request)).mapTo[Seq[CheckinWithDistance]]

  def checkinsOrderedByDistance(request: SearchNearParams): Future[Seq[CheckinWithDistance]] =
    (checkinDataActor ? CheckinDataActor.SearchNearOrderedByDistance(request)).mapTo[Seq[CheckinWithDistance]]

  def checkinsQty(request: SearchNearParams): Future[Long] =
    (checkinDataActor ? CheckinDataActor.SearchNearCount(request)).mapTo[Long]

  def getLocationCheckedInUsers(currentUserId: UserId)(userIds: Seq[UserId]): Future[LocationCheckinInfoItem] =
    (userReadActor ? UserReadActor.GetLocationCheckinInfoItem(userIds, currentUserId)).mapTo[LocationCheckinInfoItem]

  def getAllCheckedInUsers(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit): Future[Seq[UserId]] =
    (checkinDataActor ? FindAllCheckedInUsers(locationId, currentUserId, offsetLimit)).mapTo[Seq[UserId]]

  def getCheckedInFriends(locationId: LocationId, currentUserId: UserId, offsetLimit: OffsetLimit): Future[Seq[UserId]] =
    (checkinDataActor ? FindCheckedInFriends(locationId, currentUserId, offsetLimit)).mapTo[Seq[UserId]]

  def countAllCheckedInUsers(locationId: LocationId, currentUserId: UserId): Future[Long] =
    (checkinDataActor ? CountAllCheckedInUsers(locationId, currentUserId)).mapTo[Long]

  def countCheckedInFriends(locationId: LocationId, currentUserId: UserId): Future[Long] =
    (checkinDataActor ? CountCheckedInFriends(locationId, currentUserId)).mapTo[Long]

  def getCheckedInUserItems(userIds: Seq[UserId], imageSize: String): Future[Seq[CheckedInUser]] =
    (userReadActor ? UserReadActor.GetCheckedInUsers(userIds, imageSize)).mapTo[Seq[CheckedInUser]]
}

case class UsersCheckinRequest(locationId: LocationId,
                               entitiesParams: Option[Set[CheckinViewType]],
                               offsetLimit: OffsetLimit) extends Validatable{
  check

  import CheckinViewType._

  def defaultEntitiesParams = Set[CheckinViewType](UsersShort, UsersCount)

  def entitiesParameters = entitiesParams getOrElse defaultEntitiesParams
  def userParams = Set[CheckinViewType](UsersShort, UsersCount)
  def friendParams = Set[CheckinViewType](FriendsShort, FriendsCount)

  def isRelatedToUsers = entitiesParameters.intersect(userParams).nonEmpty

  def validate(ctx: ValidationContext){

    if (entitiesParameters.intersect(userParams).nonEmpty && entitiesParameters.intersect(friendParams).nonEmpty)
      ctx.fail( "Paging is denied with such entities parameter. You can get weather users or friend information, rather then both")

    if(includesUsersQty && offsetLimit.offset != 0)
      ctx.fail( "Paging is denied with such entities parameter. Quantity can be gained only with not nil offset")

    if(includesFriendsQty && offsetLimit.offset != 0)
      ctx.fail( "Paging is denied with such entities parameter. Quantity can be gained only with not nil offset")
  }

  def includesUsersQty = entitiesParameters contains UsersCount

  def includesUsersShort = entitiesParameters contains UsersShort

  def includesFriendsQty = entitiesParameters contains FriendsCount

  def includesFriendsShort = entitiesParameters contains FriendsShort
}

trait GenericCheckinUsersResponse extends SuccessfulResponse

case class CheckinUsersResponse(@JsonProp("userscount")usersCount: Option[Long],
                                users: Seq[CheckedInUser]) extends GenericCheckinUsersResponse

case class CheckinFriendsResponse(@JsonProp("friendscount")friendsCount: Option[Long],
                                  friends: Seq[CheckedInUser]) extends GenericCheckinUsersResponse

case class Checkins(usersCount: Option[Int] = None, users: Option[Seq[CheckedInUser]] = None,  friendsCount: Option[Int] = None, friends: Option[Seq[CheckedInUser]] = None)


case class UsersCheckinSearchResponse(checkin: UsersCheckinSearchLocation) extends SuccessfulResponse
case class UsersCheckinSearchLocation(location: UsersCheckinSearchLocationItem)
case class UsersCheckinSearchLocationItem(id: LocationId, name: String,
                                @JsonProp("openinghours")openingHours: String,
                                address: LocationAddressItem,
                                media: MediaUrl,
                                @JsonProp("coords")coordinates: Coordinates)

case class CheckinsSearchResponse
(
  @JsonProp("checkins_count") checkinsCount: Long,
  checkins: Seq[CheckinSearchItem]
  ) extends SuccessfulResponse

case class CheckinSearchItem
(
  id: CheckinId,
  time: Date,
  user: CheckinUserInfo,
  location: CheckinSearchLocationItem
  )

object CheckinSearchItem {
  def apply(friends: Set[UserId])(cwd: CheckinWithDistance): CheckinSearchItem =
    CheckinSearchItem(
      id = cwd.checkin.id,
      time = cwd.checkin.creationTime,
      user = CheckinUserInfo(friends)(cwd.checkin),
      location = CheckinSearchLocationItem(cwd)
    )
}

case class CheckinUserInfo
(
  @JsonProp("id") id: UserId,
  @JsonProp("name") name: String,
  @JsonProp("lastname") lastName: Option[String],
  @JsonProp("media") media: MediaUrl,
  @JsonProp("isfriend") isFriend: Option[Boolean]
  )

object CheckinUserInfo {
  def apply(friends: Set[UserId])(checkin: CheckinBaseProjection): CheckinUserInfo =
    CheckinUserInfo(
      id = checkin.user.userId,
      name = checkin.user.name,
      lastName = Some(checkin.user.lastName),
      media = checkin.user.media.asMediaUrl(Images.Checkin.Full.User.Media, UserDefaultUrlType(checkin.user.gender)),
      isFriend = if (friends contains checkin.user.userId) Some(true) else Some(false)
    )
}

case class CheckinSearchLocationItem(id: LocationId, name: String, coords: Coordinates, distance: Option[Double])
object CheckinSearchLocationItem {
  def apply(cwd: CheckinWithDistance): CheckinSearchLocationItem = {
    val l = cwd.checkin.location
    CheckinSearchLocationItem(l.locationId, l.name, l.coordinates, cwd.distance)
  }
}