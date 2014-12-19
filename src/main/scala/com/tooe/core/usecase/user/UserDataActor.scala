package com.tooe.core.usecase.user

import akka.actor.Actor
import com.tooe.core.infrastructure.BeanLookup
import com.tooe.core.usecase._
import com.tooe.core.service.UserDataService
import com.tooe.core.db.mongo.domain._
import akka.pattern.pipe
import concurrent.Future
import com.tooe.core.application.{Actors, AppActors}
import com.tooe.core.domain._
import com.tooe.api.service.{SetMainAvatar, OffsetLimit, SearchStarsRequest}
import java.util.Date
import com.tooe.core.domain.MaritalStatusId
import com.tooe.core.db.mongo.domain.UserMedia
import com.tooe.core.usecase.SearchAmongOwnFriendsRequest
import com.tooe.core.usecase.SearchUsersRequest
import com.tooe.core.domain.PresentId
import com.tooe.core.domain.RegionId
import com.tooe.core.domain.UserId
import com.tooe.core.db.mongo.domain.User
import com.tooe.core.domain.CountryId
import com.tooe.core.domain.PhotoId
import com.tooe.core.usecase.job.urls_check.ChangeUrlType
import com.fasterxml.jackson.annotation.JsonProperty

object UserDataActor{
  final val Id = Actors.UserData

  case class GetUser(id: UserId)
  case class SaveUser(user: User)
  case class RemoveUser(id: UserId)
  case class FindAndFilterUsersBy(usersId: Seq[UserId], request: SearchAmongOwnFriendsRequest, offsetLimit: OffsetLimit)
  case class CountFindAndFilterUsers(usersIds: Seq[UserId], request: SearchAmongOwnFriendsRequest)
  case class GetUsers(ids: Seq[UserId])
  case class SearchUsers(request: SearchUsersRequest)
  case class SearchUsersCount(request: SearchUsersRequest)
  case class SearchStars(request: SearchStarsRequest, offsetLimit: OffsetLimit)
  case class SearchStarsCount(request: SearchStarsRequest)
  case class AddGift(userId: UserId, presentId: PresentId)
  case class FindAbsentIds(ids: Seq[UserId])
  case class AddPhotoToUser(userId: UserId, photoId: PhotoId)
  case class UpdateUserPhotos(userId: UserId, photoIds: Seq[PhotoId])
  case class UserUpdate(userId: UserId, fields: UpdateFields)
  case class GetUserMedia(userId: UserId)

  case class FindUserIdByEmail(email: String)
  case class FindUserIdByPhone(code: String, number: String)

  case class SetStatistic(userId: UserId, updater: UpdateUserStatistic)
  case class UpdateStatistic(userId: UserId, updater: UpdateUserStatistic)
  case class UpdateStatisticForMany(userIds: Set[UserId], updater: UpdateUserStatistic)
  case class GetUserStatistics(userId: UserId)

  case class AddPhotoAlbum(userId: UserId, photoAlbumId: PhotoAlbumId)
  case class RemovePhotoAlbum(userId: UserId, photoAlbumId: PhotoAlbumId)

  case class AddNewWish(userId: UserId, wishId: WishId)
  case class RemoveWish(userId: UserId, wishId: WishId)

  case class GetUserOnlineStatus(userId: UserId)

  case class SearchUserFriends(usersIds: Seq[UserId], request: SearchUserFriendsRequest, offsetLimit: OffsetLimit)
  case class CountSearchUserFriends(usersIds: Seq[UserId], request: SearchUserFriendsRequest)

  case class SetUserMedia(userId: UserId, newMedia: Seq[UserMedia], optimisticLock: Option[Int])

  case class UpdateUserMedia(userId: UserId, userMedia: Seq[UserMedia])

  case object FindTopStars

  case class GetUserPhones(userId: UserId)
}

class UserDataActor extends Actor with AppActors{

  lazy val service = BeanLookup[UserDataService]

  import UserDataActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = {

    case SaveUser(user) => Future{ service.save(user) } pipeTo sender

    case RemoveUser(userId) => service.delete(userId)

    case GetUser(id) => Future{ service.findOne(id).getOrNotFoundException("User not found") } pipeTo sender
    case FindAndFilterUsersBy(userIds, request, offsetLimit) =>
      Future(service.findAndFilterUsersBy(userIds, request, offsetLimit)) pipeTo sender
    case CountFindAndFilterUsers(usersIds, request) => Future { service.countFindAndFilterUsers(usersIds, request) } pipeTo sender
    case GetUsers(userIds) => Future{ service.findUsersByUserIds(userIds) } pipeTo sender
    case SearchUsers(request) => Future(service.searchUsers(request)) pipeTo sender
    case SearchUsersCount(request) => Future(service.searchUsersCount(request)) pipeTo sender
    case SearchStars(request, offsetLimit) => Future(service.searchStars(request, offsetLimit)) pipeTo sender
    case SearchStarsCount(request) => Future(service.searchStarsCount(request)) pipeTo sender
    case AddGift(userId, presentId) => Future { service.addGift(userId, presentId) } pipeTo sender
    case FindAbsentIds(userIds) => Future { service.findAbsentIds(userIds) } pipeTo sender
    case AddPhotoToUser(userId, photoId) => Future { service.addPhotoToUser(userId, photoId) }
    case UpdateUserPhotos(userId, photoIds) => Future { service.updateUserLastPhotos(userId, photoIds) }
    case UserUpdate(userId, fields) => Future { service.updateUser(userId, fields) }
    case GetUserMedia(userId)  => Future { service.getUserMedia(userId) } pipeTo sender
    case SetStatistic(userId, updater) =>
      Future { service.setUserStatistic(userId, updater)} pipeTo sender
    case UpdateStatistic(userId, updater) =>
      Future { service.changeUserStatistic(userId, updater)} pipeTo sender
    case UpdateStatisticForMany(userIds, updater) =>
      Future { service.changeUsersStatistics(userIds, updater)} pipeTo sender
    case GetUserStatistics(userId) => Future { service.getUserStatistics(userId) } pipeTo sender

    case AddPhotoAlbum(userId, photoAlbumId) => Future(service.addPhotoAlbum(userId, photoAlbumId))
    case RemovePhotoAlbum(userId, photoAlbumId) => Future(service.removePhotoAlbum(userId, photoAlbumId))

    case AddNewWish(userId, wishId) => Future(service.addWish(userId, wishId))
    case RemoveWish(userId, wishId) => Future(service.removeWish(userId, wishId))
    case GetUserOnlineStatus(userId) => Future(service.getUserOnline(userId)) pipeTo sender

    case SearchUserFriends(usersIds, request, offsetLimit) => Future { service.searchUserFriends(usersIds, request, offsetLimit) } pipeTo sender
    case CountSearchUserFriends(usersIds, request) => Future { service.countSearchUserFriends(usersIds, request) } pipeTo sender

    case SetUserMedia(userId, newMedia, optimisticLock) => Future { service.updateUserMedia(userId, newMedia, optimisticLock) } pipeTo sender

    case FindUserIdByEmail(email) => Future { service.findUserIdByEmail(email) } pipeTo sender
    case FindUserIdByPhone(code, number) => Future(service.findUserIdByPhone(code = code, number = number)) pipeTo sender

    case msg: ChangeUrlType.ChangeTypeToS3 => Future { service.updateMediaStorageToS3(UserId(msg.url.entityId), msg.url.mediaId, msg.newMediaId) }

    case msg: ChangeUrlType.ChangeTypeToCDN => Future { service.updateMediaStorageToCDN(UserId(msg.url.entityId), msg.url.mediaId) }

    case FindTopStars => Future { service.findTopStars } pipeTo sender

    case GetUserPhones(userId) => Future(service.findUserPhones(userId) getOrNotFound (userId, "User")) pipeTo sender
  }
}

case class UserInfoAddress(country: String, region: String)

case class UpdateFields(name: Option[String],
                        lastName: Option[String],
                        email: Option[String],
                        regionId: Option[RegionId],
                        countryId: Option[CountryId],
                        regionName: Option[String],
                        countryName: Option[String],
                        birthday: Unsetable[Date],
                        gender: Option[Gender],
                        maritalStatus: Unsetable[MaritalStatusId],
                        education: Unsetable[String],
                        job: Unsetable[String],
                        aboutMe: Unsetable[String],
                        settings: Option[UserSettingsProfileItem],
                        messageSettings: Option[UserMessageSettingUpdateItem],
                        onlineStatus: Unsetable[OnlineStatusId],
                        phones: UpdatePhonesFields
                        )

case class UpdatePhonesFields(main: Unsetable[PhoneShort], all: Unsetable[Seq[PhoneShort]])

case class UserMessageSettingUpdateItem(@JsonProperty("showtext")showText: Unsetable[Boolean],
                                        @JsonProperty("playaudio")playAudio: Unsetable[Boolean],
                                        @JsonProperty("sendemail")sendEmail: Unsetable[UserSendEmailEvent])
